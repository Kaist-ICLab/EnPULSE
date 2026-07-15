package kaist.iclab.mobiletracker.viewmodels.data

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.repository.DataRepository
import kaist.iclab.mobiletracker.repository.SensorInfo
import kaist.iclab.mobiletracker.utils.AppToast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import kaist.iclab.mobiletracker.utils.SupabaseLoadingInterceptor

/**
 * Data class to track the progress of uploading all sensors.
 */
data class UploadProgressState(
    val isComplete: Boolean = false,
    val currentSensorName: String = "",
    val currentIndex: Int = 0,
    val totalSensors: Int = 0,
    val successCount: Int = 0,
    val failedCount: Int = 0,
    val upToDateCount: Int = 0,
    val successfulSensors: List<String> = emptyList(),
    val failedSensors: List<String> = emptyList(),
    val upToDateSensors: List<String> = emptyList()
)

/**
 * UI state for the Data screen.
 */
data class DataUiState(
    val isLoading: Boolean = true,
    val sensors: List<SensorInfo> = emptyList(),
    val totalRecords: Int = 0,
    val error: String? = null,
    val isUploading: Boolean = false,
    val isDeleting: Boolean = false,
    val isExporting: Boolean = false,
    val currentTime: String = "--",
    val lastWatchData: String? = null,
    val lastSuccessfulUpload: String? = null,
    val uploadProgress: UploadProgressState? = null
)

/**
 * ViewModel for the Data screen.
 * Provides sensor list with record counts and last recorded times.
 */
class DataViewModel(
    private val dataRepository: DataRepository,
    private val dataExportHelper: kaist.iclab.mobiletracker.helpers.DataExportHelper,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(DataUiState())
    val uiState: StateFlow<DataUiState> = _uiState.asStateFlow()

    private val prefs = context.getSharedPreferences("sync_timestamps", Context.MODE_PRIVATE)
    private val dateFormat =
        java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())

    init {
        loadSensorInfo()
        startTimestampUpdates()
    }

    private fun startTimestampUpdates() {
        viewModelScope.launch {
            while (true) {
                val currentTime = dateFormat.format(java.util.Date())
                val lastWatch = prefs.getLong("last_watch_data", 0L).let {
                    if (it > 0) dateFormat.format(java.util.Date(it)) else null
                }
                val lastUpload = prefs.getLong("last_successful_upload", 0L).let {
                    if (it > 0) dateFormat.format(java.util.Date(it)) else null
                }

                _uiState.value = _uiState.value.copy(
                    currentTime = currentTime,
                    lastWatchData = lastWatch,
                    lastSuccessfulUpload = lastUpload
                )
                kotlinx.coroutines.delay(1000L)
            }
        }
    }

    /**
     * Load sensor information from the repository.
     */
    fun loadSensorInfo() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val sensors = dataRepository.getAllSensorInfo()
                val totalRecords = sensors.sumOf { it.recordCount }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    sensors = sensors,
                    totalRecords = totalRecords
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load sensor data"
                )
            }
        }
    }

    /**
     * Refresh the sensor list.
     */
    fun refresh() {
        loadSensorInfo()
    }

    /**
     * Upload all data for all sensors with detailed progress tracking.
     */
    fun uploadAllData() {
        if (_uiState.value.uploadProgress != null) return

        viewModelScope.launch {
            val sensorsToUpload = _uiState.value.sensors.filter { it.recordCount > 0 }
            if (sensorsToUpload.isEmpty()) {
                AppToast.show(context, R.string.toast_no_data_to_upload)
                return@launch
            }

            startUploadProgress(sensorsToUpload.size)
            val successfulSensors = mutableListOf<String>()
            val failedSensors = mutableListOf<String>()
            val upToDateSensors = mutableListOf<String>()

            try {
                sensorsToUpload.forEachIndexed { index, sensor ->
                    updateUploadProgress(index + 1, sensor.displayName)

                    try {
                        val result = dataRepository.uploadSensorData(sensor.sensorId)
                        if (result > 0) {
                            successfulSensors.add(sensor.displayName)
                        } else if (result == -1) {
                            failedSensors.add(sensor.displayName)
                        } else {
                            upToDateSensors.add(sensor.displayName)
                        }
                    } catch (e: Exception) {
                        failedSensors.add(sensor.displayName)
                        Log.e("DataViewModel", "Error uploading ${sensor.sensorId}", e)
                    }
                }
            } finally {
                finishUploadProgress(successfulSensors, failedSensors, upToDateSensors)
                loadSensorInfo()
            }
        }
    }

    private fun startUploadProgress(totalSensors: Int) {
        SupabaseLoadingInterceptor.suppressGlobalLoading = true
        _uiState.value = _uiState.value.copy(
            uploadProgress = UploadProgressState(
                isComplete = false,
                totalSensors = totalSensors
            )
        )
    }

    private fun updateUploadProgress(currentIndex: Int, sensorName: String) {
        _uiState.value = _uiState.value.copy(
            uploadProgress = _uiState.value.uploadProgress?.copy(
                currentIndex = currentIndex,
                currentSensorName = sensorName
            )
        )
    }

    private fun finishUploadProgress(successful: List<String>, failed: List<String>, upToDate: List<String>) {
        SupabaseLoadingInterceptor.suppressGlobalLoading = false
        _uiState.value = _uiState.value.copy(
            uploadProgress = _uiState.value.uploadProgress?.copy(
                isComplete = true,
                successCount = successful.size,
                failedCount = failed.size,
                upToDateCount = upToDate.size,
                successfulSensors = successful,
                failedSensors = failed,
                upToDateSensors = upToDate
            )
        )
    }

    /**
     * Dismisses the upload progress summary dialog.
     */
    fun clearUploadProgress() {
        _uiState.value = _uiState.value.copy(uploadProgress = null)
    }

    /**
     * Delete all data for all sensors.
     */
    fun deleteAllData() {
        if (_uiState.value.isDeleting) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true)
            try {
                dataRepository.deleteAllAllData()
                AppToast.show(context, R.string.toast_data_deleted)
                loadSensorInfo()
            } catch (e: Exception) {
                AppToast.show(context, R.string.toast_error_generic)
            } finally {
                _uiState.value = _uiState.value.copy(isDeleting = false)
            }
        }
    }


    /**
     * Export all sensor data to a ZIP file containing CSVs.
     */
    fun exportAllToCsv() {
        if (_uiState.value.isExporting) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true)
            try {
                val zipFile = dataExportHelper.exportAllData(context)

                if (zipFile != null && zipFile.exists()) {
                    shareExportFile(zipFile)
                } else {
                    AppToast.show(context, R.string.toast_export_failed)
                }
            } catch (e: Exception) {
                Log.e("DataViewModel", "Error exporting all data", e)
                AppToast.show(context, R.string.toast_export_failed)
            } finally {
                _uiState.value = _uiState.value.copy(isExporting = false)
            }
        }
    }

    private fun shareExportFile(file: java.io.File) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                putExtra(android.content.Intent.EXTRA_SUBJECT, "Mobile Tracker Data Export")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = android.content.Intent.createChooser(shareIntent, "Share Data Export")
            chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e("DataViewModel", "Error sharing export file", e)
            AppToast.show(context, R.string.toast_export_failed)
        }
    }
}
