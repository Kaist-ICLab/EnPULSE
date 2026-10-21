package kaist.iclab.mobiletracker.viewmodels.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.db.obx.SurveyResponseStore
import kaist.iclab.mobiletracker.db.obx.WebAppLogStore
import kaist.iclab.mobiletracker.repository.PhoneSensorRepository
import kaist.iclab.mobiletracker.repository.WatchSensorRepository
import kaist.iclab.mobiletracker.repository.onFailure
import kaist.iclab.mobiletracker.services.SyncTimestampService
import kaist.iclab.mobiletracker.services.upload.SensorUploadService
import kaist.iclab.mobiletracker.utils.DateTimeFormatter
import kaist.iclab.mobiletracker.utils.SensorTypeHelper
import kaist.iclab.tracker.sensor.core.Sensor
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class DataSyncSettingsViewModel(
    private val phoneSensorRepository: PhoneSensorRepository,
    private val watchSensorRepository: WatchSensorRepository,
    private val timestampService: SyncTimestampService,
    private val sensors: List<Sensor<*, *>>,
    private val sensorUploadService: SensorUploadService,
    private val webAppLogStore: WebAppLogStore,
    private val surveyResponseStore: SurveyResponseStore,
) : ViewModel() {
    private val TAG = "ServerSyncSettingsViewModel"

    // One-shot UI events (e.g. toasts), collected by the host screen.
    private val _uiEvent = MutableSharedFlow<DataSyncSettingsUiEvent>()
    val uiEvent: SharedFlow<DataSyncSettingsUiEvent> = _uiEvent.asSharedFlow()

    // Current time (updates every second)
    private val _currentTime = MutableStateFlow(DateTimeFormatter.getCurrentTimeFormatted())
    val currentTime: StateFlow<String> = _currentTime.asStateFlow()

    // Last watch data received
    private val _lastWatchData = MutableStateFlow<String?>(null)
    val lastWatchData: StateFlow<String?> = _lastWatchData.asStateFlow()

    // Last successful upload
    private val _lastSuccessfulUpload = MutableStateFlow<String?>(null)
    val lastSuccessfulUpload: StateFlow<String?> = _lastSuccessfulUpload.asStateFlow()

    // Flush operation state
    private val _isFlushing = MutableStateFlow(false)
    val isFlushing: StateFlow<Boolean> = _isFlushing.asStateFlow()

    init {
        // Update current time every second
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000.milliseconds)
                _currentTime.value = DateTimeFormatter.getCurrentTimeFormatted()
            }
        }

        // Load timestamps from service
        loadTimestamps()

        // Refresh timestamps periodically (every 5 seconds)
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(5000.milliseconds)
                loadTimestamps()
            }
        }
    }

    /**
     * Load all timestamps from SyncTimestampService
     */
    private fun loadTimestamps() {
        _lastWatchData.value = timestampService.getLastWatchDataReceived()
        _lastSuccessfulUpload.value = timestampService.getLastSuccessfulUpload()
    }
}

/**
 * One-shot UI events emitted by [DataSyncSettingsViewModel].
 */
sealed interface DataSyncSettingsUiEvent {
    data class ShowToast(
        val messageResId: Int,
        val formatArgs: List<Any> = emptyList()
    ) : DataSyncSettingsUiEvent
}

