package kaist.iclab.mobiletracker.viewmodels.data

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.repository.DataRepository
import kaist.iclab.mobiletracker.repository.SensorInfo
import kaist.iclab.mobiletracker.services.SyncTimestampService
import kaist.iclab.mobiletracker.services.upload.DataUploadService
import kaist.iclab.mobiletracker.services.upload.DataUploadState
import kaist.iclab.mobiletracker.services.upload.SensorUploadResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import kotlin.time.Duration.Companion.milliseconds

/**
 * Data class to track the progress of uploading all sensors.
 */
data class UploadProgressState(
    val isComplete: Boolean = false,
    val currentSensorName: String = "",
    val currentBatch: Int = 0,
    val totalBatches: Int = 0,
    /** Per-sensor record counts once the run finishes — see [SensorUploadResult]. */
    val results: List<SensorUploadResult> = emptyList(),
    val upToDateCount: Int = 0,
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
    private val syncTimestampService: SyncTimestampService,
    private val context: Context
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
        observeUploadService()
    }

    /**
     * Mirrors [DataUploadService]'s shared upload state/events into [uiState] so the progress
     * bar shows the same thing regardless of whether the upload was triggered by this screen's
     * "Upload Now" button or by an automatic sync running in the background.
     */
    private fun observeUploadService() {
        viewModelScope.launch {
            DataUploadService.uploadState.collect { state ->
                when (state) {
                    is DataUploadState.InProgress -> {
                        _uiState.value = _uiState.value.copy(
                            uploadProgress = UploadProgressState(
                                isComplete = false,
                                currentSensorName = state.currentSensorName,
                                currentBatch = state.currentBatch,
                                totalBatches = state.totalBatches
                            )
                        )
                    }

                    // The run is over. Normally the completion event has already replaced this
                    // with the finished summary, and that must survive — so only a still-running
                    // snapshot is dropped. Without this the in-progress state was a dead end:
                    // if the summary never arrived (or arrived before this ViewModel existed),
                    // the indicator kept spinning and DataScreen kept the Upload button disabled
                    // with no way back, since clearUploadProgress() is only reachable from the
                    // summary dialog.
                    DataUploadState.Idle -> {
                        if (_uiState.value.uploadProgress?.isComplete == false) {
                            _uiState.value = _uiState.value.copy(uploadProgress = null)
                        }
                    }
                }
            }
        }
        viewModelScope.launch {
            DataUploadService.completionEvents.collect { summary ->
                _uiState.value = _uiState.value.copy(
                    uploadProgress = UploadProgressState(
                        isComplete = true,
                        results = summary.results,
                        upToDateCount = summary.upToDateCount,
                        upToDateSensors = summary.upToDateSensors
                    )
                )
                loadSensorInfo()
            }
        }
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
     * Upload all data for all sensors via [DataUploadService]'s foreground service — same
     * upload path auto-sync uses, so it runs without blocking the UI. Progress and the
     * completion summary arrive through [observeUploadService].
     */
    fun uploadAllData() {
        if (DataUploadService.uploadState.value is DataUploadState.InProgress) return

        viewModelScope.launch {
            val sensorsToUpload = _uiState.value.sensors.filter { it.recordCount > 0 }
            if (sensorsToUpload.isEmpty()) {
                _uiEvent.emit(DataUiEvent.ShowToast(R.string.toast_no_data_to_upload))
                return@launch
            }

            DataUploadService.start(context, sensorsToUpload.map { it.sensorId })
        }
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
