package kaist.iclab.mobiletracker.viewmodels.settings

import android.content.Context
import android.content.res.Configuration
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.repository.PhoneSensorRepository
import kaist.iclab.mobiletracker.repository.Result
import kaist.iclab.mobiletracker.repository.WatchSensorRepository
import kaist.iclab.mobiletracker.services.SyncTimestampService
import kaist.iclab.mobiletracker.services.upload.PhoneSensorUploadService
import kaist.iclab.mobiletracker.services.upload.WatchSensorUploadService
import kaist.iclab.mobiletracker.utils.AppToast
import kaist.iclab.mobiletracker.utils.DateTimeFormatter
import kaist.iclab.mobiletracker.utils.SensorTypeHelper
import kaist.iclab.tracker.sensor.core.Sensor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import java.text.NumberFormat
import java.util.Locale

class DataSyncSettingsViewModel(
    private val phoneSensorRepository: PhoneSensorRepository,
    private val watchSensorRepository: WatchSensorRepository,
    private val timestampService: SyncTimestampService,
    private val context: Context
) : ViewModel(), KoinComponent {
    private val sensors: List<Sensor<*, *>> by inject(qualifier = named("phoneSensors"))
    private val phoneSensorUploadService: PhoneSensorUploadService by inject()
    private val watchSensorUploadService: WatchSensorUploadService by inject()
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

            // Delete all phone sensor data
            val phoneResult = phoneSensorRepository.flushAllData()

            // Delete all watch sensor data
            val watchResult = watchSensorRepository.flushAllData()

            // Check if both operations succeeded
            when {
                phoneResult is Result.Success && watchResult is Result.Success -> {
                    // Both deletions succeeded
                    // Clear all sync-related timestamps (except next scheduled upload)
                    timestampService.clearAllSyncTimestamps()
                    AppToast.show(context, R.string.toast_data_deleted)
                }

                phoneResult is Result.Error -> {
                    Log.e(
                        TAG,
                        "Error flushing phone sensor data: ${phoneResult.message}",
                        phoneResult.exception
                    )
                    // Still try to clear timestamps even if deletion partially failed
                    timestampService.clearAllSyncTimestamps()
                }

                watchResult is Result.Error -> {
                    Log.e(
                        TAG,
                        "Error flushing watch sensor data: ${watchResult.message}",
                        watchResult.exception
                    )
                    // Still try to clear timestamps even if deletion partially failed
                    timestampService.clearAllSyncTimestamps()
                }
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

                // Upload all phone sensors
                sensors.forEach { sensor ->
                    val sensorId = sensor.id
                    
                    try {
                        // Check if data is available (but don't show toast for individual sensors)
                        if (!phoneSensorUploadService.hasDataToUpload(sensorId)) {
                            skippedCount++
                        } else {
                            // Upload data using the upload service
                            when (val result =
                                phoneSensorUploadService.uploadSensorData(sensorId)) {
                                is Result.Success -> {
                                    uploadedCount++
                                }
                                is Result.Error -> {
                                    failedCount++
                                    Log.e(
                                        TAG,
                                        "Error uploading sensor data for ${sensor.name}: ${result.message}",
                                        result.exception
                                    )
                                }
                            }
                        }
                    } catch (e: Exception) {
                        failedCount++
                        Log.e(TAG, "Exception uploading sensor data for ${sensor.name}: ${e.message}", e)
                    }
                }
                
                // Upload all watch sensors
                SensorTypeHelper.watchSensorIds.forEach { sensorId ->
                    try {
                        if (!watchSensorUploadService.hasDataToUpload(sensorId)) {
                            skippedCount++
                        } else {
                            when (val result = watchSensorUploadService.uploadSensorData(sensorId)) {
                                is Result.Success -> {
                                    uploadedCount++
                                }
                                is Result.Error -> {
                                    failedCount++
                                    Log.e(TAG, "Error uploading watch sensor data for $sensorId: ${result.message}", result.exception)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        failedCount++
                        Log.e(TAG, "Exception uploading watch sensor data for $sensorId: ${e.message}", e)
                    }
                }
                
                // Show summary toast
                when {
                    uploadedCount > 0 -> {
                        // Show how many sensors successfully uploaded
                        AppToast.show(
                            context,
                            R.string.toast_upload_all_summary,
                            uploadedCount
                        )
                    }
                    skippedCount == totalSensorsCount -> {
                        // All sensors skipped (no new data)
                        AppToast.show(context, R.string.toast_no_data_to_upload)
                    }
                    failedCount > 0 && uploadedCount == 0 -> {
                        // All attempts failed and none were successful
                        AppToast.show(context, R.string.toast_sensor_data_upload_error)
                    }
                }
                
                // Refresh timestamps
                loadTimestamps()
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading all sensor data: ${e.message}", e)
                AppToast.show(context, R.string.toast_sensor_data_upload_error)
            }
        }
    }
}

