package kaist.iclab.wearabletracker.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import kaist.iclab.tracker.listener.SamsungHealthSensorInitializer
import kaist.iclab.tracker.sensor.controller.BackgroundController
import kaist.iclab.tracker.sensor.controller.ControllerState
import kaist.iclab.tracker.sensor.core.SensorState
import kaist.iclab.tracker.sensor.galaxywatch.ECGSensor
import kaist.iclab.wearabletracker.data.AutoSyncManager
import kaist.iclab.wearabletracker.data.CampaignSensorConfigRepository
import kaist.iclab.wearabletracker.data.DeviceInfo
import kaist.iclab.wearabletracker.data.PhoneCommunicationManager
import kaist.iclab.wearabletracker.helpers.NotificationHelper
import kaist.iclab.wearabletracker.repository.Result
import kaist.iclab.wearabletracker.repository.WatchSensorRepository
import kaist.iclab.wearabletracker.storage.SensorDataReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class SettingsViewModel(
    private val sensorController: BackgroundController,
    private val sensorDataReceiver: SensorDataReceiver,
    private val phoneCommunicationManager: PhoneCommunicationManager,
    private val repository: WatchSensorRepository,
    private val samsungHealthSensorInitializer: SamsungHealthSensorInitializer,
    private val applicationContext: Context,
    private val autoSyncManager: AutoSyncManager,
    private val ecgSensor: ECGSensor,
    private val campaignSensorConfigRepository: CampaignSensorConfigRepository
) : ViewModel() {
    companion object {
        private val TAG = SettingsViewModel::class.simpleName
        private const val RECORD_COUNT_REFRESH_MS = 10_000L // 10 seconds
        private const val BATTERY_REFRESH_MS = 30_000L // 30 seconds
        private const val PHONE_STATUS_REFRESH_MS = 15_000L // 15 seconds
    }

    // StateFlow for last sync timestamp
    private val _lastSyncTimestamp = MutableStateFlow<Long?>(null)
    val lastSyncTimestamp: StateFlow<Long?> = _lastSyncTimestamp.asStateFlow()

    // Total record count across all sensors
    private val _totalRecordCount = MutableStateFlow(0)
    val totalRecordCount: StateFlow<Int> = _totalRecordCount.asStateFlow()

    // Sync progress: 0.0 to 1.0, null if not syncing
    val syncProgress: StateFlow<Float?> = phoneCommunicationManager.syncProgress

    // Phone connection status
    private val _isPhoneConnected = MutableStateFlow(false)
    val isPhoneConnected: StateFlow<Boolean> = _isPhoneConnected.asStateFlow()

    private val nodeClient by lazy { Wearable.getNodeClient(applicationContext) }

    // Auto-sync settings — no separate enabled flag surfaced to the UI; see setAutoSyncInterval.
    val autoSyncInterval: StateFlow<Long> = repository.autoSyncIntervalFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    /**
     * Auto-sync has no separate on/off switch in the UI — picking an interval of 0 (None) is
     * what turns it off, so enabled is derived from the interval here.
     */
    fun setAutoSyncInterval(intervalMs: Long) {
        repository.setAutoSyncInterval(intervalMs)
        repository.setAutoSyncEnabled(intervalMs > 0L)
    }

    // Battery level (0-100)
    private val _batteryLevel = MutableStateFlow(-1)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    // Recording start time (null when not recording)
    private val _recordingStartTime = MutableStateFlow<Long?>(null)
    val recordingStartTime: StateFlow<Long?> = _recordingStartTime.asStateFlow()

    // Samsung Health connection state - Start button should be disabled when false
    val isSamsungHealthConnected: StateFlow<Boolean> =
        samsungHealthSensorInitializer.connectionStateFlow

    // SDK Policy Error state - true when dev mode is not enabled on Health Platform
    val sdkPolicyError: StateFlow<Boolean> = samsungHealthSensorInitializer.sdkPolicyErrorStateFlow

    // ECG is on-demand only and isn't part of `sensorState` (not in watchSensorDescriptors);
    // this drives visibility of the "Measure ECG" button. Also requires ECG to be part of the
    // wearer's campaign (a never-synced/null config fails open).
    val ecgAvailable: StateFlow<Boolean> = combine(
        ecgSensor.sensorStateFlow,
        campaignSensorConfigRepository.configFlow
    ) { sensorState, config ->
        sensorState.flag != SensorState.FLAG.UNAVAILABLE &&
            (config.activeSensorIds == null || ecgSensor.id in config.activeSensorIds)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Names (not bare ids) of sensors active for the wearer's campaign, translated via
    // `sensorController.sensors` since `sensorState`/`sensorMap` below are keyed by `.name`
    // (the human-readable display form, e.g. "Skin Temperature") rather than `.id`.
    // Null means "never synced with the phone" and fails open (shows every sensor).
    val activeCampaignSensorNames: StateFlow<Set<String>?> = campaignSensorConfigRepository.configFlow
        .map { config ->
            config.activeSensorIds?.let { ids ->
                sensorController.sensors.filter { it.id in ids }.map { it.name }.toSet()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val ecgPermissions: Array<String> get() = ecgSensor.permissions

    /**
     * Clear the SDK Policy Error and stop logging (called when user dismisses the error screen).
     */
    fun clearSdkPolicyError() {
        sensorController.stop()
        samsungHealthSensorInitializer.clearSdkPolicyError()
    }

    init {
        // ECG is excluded from BackgroundController's managed sensor list (see KoinModule.kt),
        // so nothing else calls init() on it — without this, its persisted state stays at the
        // default UNAVAILABLE forever and the "Measure ECG" button never appears.
        ecgSensor.init()

        viewModelScope.launch {
            repository.lastSyncTimestampFlow.collect {
                _lastSyncTimestamp.value = it
            }
        }

        viewModelScope.launch {
            sensorController.controllerStateFlow.collect {
                when (it.flag) {
                    ControllerState.FLAG.RUNNING -> {
                        sensorDataReceiver.startBackgroundCollection()
                        // Track recording start time
                        if (_recordingStartTime.value == null) {
                            _recordingStartTime.value = System.currentTimeMillis()
                        }
                    }
                    ControllerState.FLAG.PAUSED -> {
                        // Sensors are paused (watch not worn) — keep SensorDataReceiver
                        // alive since no data arrives anyway, and preserve recording
                        // start time so the elapsed timer isn't reset on resume.
                    }
                    else -> {
                        sensorDataReceiver.stopBackgroundCollection()
                        _recordingStartTime.value = null
                    }
                }
            }
        }

        // Stop controller immediately when SDK Policy Error is detected
        viewModelScope.launch {
            sdkPolicyError.collect { hasError ->
                if (hasError) {
                    sensorController.stop()
                }
            }
        }

        // Periodic record count refresh
        viewModelScope.launch {
            while (true) {
                refreshRecordCount()
                delay(RECORD_COUNT_REFRESH_MS)
            }
        }

        // Periodic battery level refresh
        viewModelScope.launch {
            while (true) {
                refreshBatteryLevel()
                delay(BATTERY_REFRESH_MS)
            }
        }

        // Periodic phone connection status refresh
        viewModelScope.launch {
            while (true) {
                refreshPhoneConnectionStatus()
                delay(PHONE_STATUS_REFRESH_MS)
            }
        }
    }

    val sensorMap = sensorController.sensors.associateBy { it.name }
    val sensorState = sensorController.sensors.associate { it.name to it.sensorStateFlow }
    val controllerState = sensorController.controllerStateFlow

    fun update(sensorName: String, status: Boolean) {
        val sensor = sensorMap[sensorName] ?: run {
            Log.w(TAG, "Sensor not found: $sensorName")
            return
        }
        if (status) sensor.enable()
        else sensor.disable()
    }

    fun getDeviceInfo(context: Context, callback: (DeviceInfo) -> Unit) {
        Wearable.getNodeClient(context).localNode
            .addOnSuccessListener { localNode ->
                val deviceInfo = DeviceInfo(
                    name = localNode.displayName,
                    id = localNode.id
                )
                callback(deviceInfo)
            }
            .addOnFailureListener { exception ->
                Log.e(
                    TAG,
                    "Error getting device information from getDeviceInfo(): ${exception.message}",
                    exception
                )
                NotificationHelper.showException(
                    context,
                    exception,
                    "Failed to get device information"
                )
            }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun startLogging() {
        sensorController.start()
    }

    fun stopLogging() {
        sensorController.stop()
    }

    fun upload() {
        viewModelScope.launch {
            phoneCommunicationManager.sendDataToPhone()
        }
    }

    fun refreshLastSyncTimestamp() {
        // Reactive via lastSyncTimestampFlow
    }

    fun flush(context: Context) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.deleteAllSensorData()
            }

            when (result) {
                is Result.Success -> {
                    NotificationHelper.showFlushSuccess(context)
                    refreshRecordCount()
                }

                is Result.Error -> {
                    NotificationHelper.showFlushFailure(
                        context,
                        result.exception,
                        "Failed to delete sensor data"
                    )
                }
            }
        }
    }

    private suspend fun refreshRecordCount() {
        try {
            val count = withContext(Dispatchers.IO) {
                repository.getTotalRecordCount()
            }
            _totalRecordCount.value = count
        } catch (e: Exception) {
            Log.w(TAG, "Failed to refresh record count: ${e.message}")
        }
    }

    private fun refreshBatteryLevel() {
        try {
            val batteryStatus =
                applicationContext.registerReceiver(
                    null,
                    IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                )
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            _batteryLevel.value = if (level >= 0) (level * 100) / scale else -1
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read battery level: ${e.message}")
        }
    }

    private suspend fun refreshPhoneConnectionStatus() {
        try {
            val nodes = suspendCancellableCoroutine<List<Node>> { continuation ->
                nodeClient.connectedNodes
                    .addOnSuccessListener { continuation.resume(it) }
                    .addOnFailureListener { continuation.resumeWithException(it) }
            }
            _isPhoneConnected.value = nodes.isNotEmpty()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check phone connection: ${e.message}")
            _isPhoneConnected.value = false
        }
    }
}