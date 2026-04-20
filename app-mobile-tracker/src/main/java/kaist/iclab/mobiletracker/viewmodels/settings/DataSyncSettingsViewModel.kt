package kaist.iclab.mobiletracker.viewmodels.settings

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.repository.PhoneSensorRepository
import kaist.iclab.mobiletracker.repository.WatchSensorRepository
import kaist.iclab.mobiletracker.repository.onFailure
import kaist.iclab.mobiletracker.repository.onSuccess
import kaist.iclab.mobiletracker.services.SyncTimestampService
import kaist.iclab.mobiletracker.services.upload.PhoneSensorUploadService
import kaist.iclab.mobiletracker.services.upload.WatchSensorUploadService
import kaist.iclab.mobiletracker.utils.AppToast
import kaist.iclab.mobiletracker.utils.DateTimeFormatter
import kaist.iclab.mobiletracker.utils.SensorTypeHelper
import kaist.iclab.tracker.sensor.core.Sensor
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DataSyncSettingsViewModel(
    private val phoneSensorRepository: PhoneSensorRepository,
    private val watchSensorRepository: WatchSensorRepository,
    private val timestampService: SyncTimestampService,
    private val sensors: List<Sensor<*, *>>,
    private val phoneSensorUploadService: PhoneSensorUploadService,
    private val watchSensorUploadService: WatchSensorUploadService,
    private val context: Context
) : ViewModel() {
    private val TAG = "ServerSyncSettingsViewModel"

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
                kotlinx.coroutines.delay(1000)
                _currentTime.value = DateTimeFormatter.getCurrentTimeFormatted()
            }
        }

        // Load timestamps from service
        loadTimestamps()

        // Refresh timestamps periodically (every 5 seconds)
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(5000)
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

    /**
     * Flush all local sensor data
     */
    fun flushAllData() {
        viewModelScope.launch {
            _isFlushing.value = true

            val phoneResult = phoneSensorRepository.flushAllData()
            val watchResult = watchSensorRepository.flushAllData()

            phoneResult.onFailure { e ->
                Log.e(TAG, "Error flushing phone sensor data: ${e.message}", e)
            }
            watchResult.onFailure { e ->
                Log.e(TAG, "Error flushing watch sensor data: ${e.message}", e)
            }

            // Clear timestamps regardless of partial failure
            timestampService.clearAllSyncTimestamps()

            if (phoneResult.isSuccess && watchResult.isSuccess) {
                AppToast.show(context, R.string.toast_data_deleted)
            }

            _isFlushing.value = false
        }
    }

    /**
     * Upload all sensor data (both phone and watch) to Supabase.
     * Uses per-sensor upload logic (including SyncTimestampService) to avoid re-uploading
     * previously sent data, so this operation is idempotent across runs.
     */
    fun uploadAllSensorData() {
        viewModelScope.launch {
            try {
                var uploadedCount = 0
                var skippedCount = 0
                var failedCount = 0

                val totalSensorsCount = sensors.size + SensorTypeHelper.watchSensorIds.size

                // Upload all phone sensors in parallel
                val phoneJobs = sensors.map { sensor ->
                    viewModelScope.async {
                        if (!phoneSensorUploadService.hasDataToUpload(sensor.id)) {
                            skippedCount++
                        } else {
                            phoneSensorUploadService.uploadSensorData(sensor.id)
                                .onSuccess { _: Unit -> uploadedCount++ }
                                .onFailure { e: Throwable ->
                                    failedCount++
                                    Log.e(TAG, "Upload failed for ${sensor.name}: ${e.message}", e)
                                }
                        }
                    }
                }

                // Upload all watch sensors in parallel
                val watchJobs = SensorTypeHelper.watchSensorIds.map { sensorId ->
                    viewModelScope.async {
                        if (!watchSensorUploadService.hasDataToUpload(sensorId)) {
                            skippedCount++
                        } else {
                            watchSensorUploadService.uploadSensorData(sensorId)
                                .onSuccess { _: Unit -> uploadedCount++ }
                                .onFailure { e: Throwable ->
                                    failedCount++
                                    Log.e(TAG, "Upload failed for $sensorId: ${e.message}", e)
                                }
                        }
                    }
                }

                // Wait for all uploads to complete
                (phoneJobs + watchJobs).awaitAll()

                // Show summary toast
                when {
                    uploadedCount > 0 -> {
                        AppToast.show(context, R.string.toast_upload_all_summary, uploadedCount)
                    }

                    skippedCount == totalSensorsCount -> {
                        AppToast.show(context, R.string.toast_no_data_to_upload)
                    }

                    failedCount > 0 && uploadedCount == 0 -> {
                        AppToast.show(context, R.string.toast_sensor_data_upload_error)
                    }
                }

                loadTimestamps()
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading all sensor data: ${e.message}", e)
                AppToast.show(context, R.string.toast_sensor_data_upload_error)
            }
        }
    }
}

