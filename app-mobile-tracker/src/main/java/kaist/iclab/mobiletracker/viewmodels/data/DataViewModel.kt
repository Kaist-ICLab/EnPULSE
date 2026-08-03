package kaist.iclab.mobiletracker.viewmodels.data

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.repository.DataRepository
import kaist.iclab.mobiletracker.repository.SensorInfo
import kaist.iclab.mobiletracker.services.SyncTimestampService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext

import kaist.iclab.mobiletracker.utils.SupabaseLoadingInterceptor
import kotlin.time.Duration.Companion.milliseconds

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
    private val syncTimestampService: SyncTimestampService
) : ViewModel() {

    private val _uiState = MutableStateFlow(DataUiState())
    val uiState: StateFlow<DataUiState> = _uiState.asStateFlow()

    // One-shot UI events (e.g. toasts, share sheet), collected by the host screen.
    private val _uiEvent = MutableSharedFlow<DataUiEvent>()
    val uiEvent: SharedFlow<DataUiEvent> = _uiEvent.asSharedFlow()

    private val dateFormat =
        java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())

    init {
        loadSensorInfo()
        startTimestampUpdates()
    }

    private fun startTimestampUpdates() {
        viewModelScope.launch {
            while (true) {
                _uiState.value = _uiState.value.copy(
                    currentTime = dateFormat.format(java.util.Date()),
                    lastWatchData = syncTimestampService.getLastWatchDataReceived(),
                    lastSuccessfulUpload = syncTimestampService.getLastSuccessfulUpload()
                )
                kotlinx.coroutines.delay(1000L.milliseconds)
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
                val sensors = withContext(Dispatchers.IO) { dataRepository.getAllSensorInfo() }
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
                _uiEvent.emit(DataUiEvent.ShowToast(R.string.toast_no_data_to_upload))
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
                        if (e is CancellationException) throw e
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
                _uiEvent.emit(DataUiEvent.ShowToast(R.string.toast_data_deleted))
                loadSensorInfo()
            } catch (_: Exception) {
                _uiEvent.emit(DataUiEvent.ShowToast(R.string.toast_error_generic))
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
                val zipFile = dataExportHelper.exportAllData()

                if (zipFile != null && zipFile.exists()) {
                    dataExportHelper.shareZip(zipFile)
                } else {
                    _uiEvent.emit(DataUiEvent.ShowToast(R.string.toast_export_failed))
                }
            } catch (e: Exception) {
                Log.e("DataViewModel", "Error exporting all data", e)
                _uiEvent.emit(DataUiEvent.ShowToast(R.string.toast_export_failed))
            } finally {
                _uiState.value = _uiState.value.copy(isExporting = false)
            }
        }
    }
}

/**
 * One-shot UI events emitted by [DataViewModel].
 */
sealed interface DataUiEvent {
    data class ShowToast(val messageResId: Int) : DataUiEvent
}
