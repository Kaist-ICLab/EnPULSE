package kaist.iclab.mobiletracker.viewmodels.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kaist.iclab.mobiletracker.repository.CampaignSensorRepository
import kaist.iclab.mobiletracker.services.AutoSyncService
import kaist.iclab.mobiletracker.services.SyncTimestampService
import kaist.iclab.mobiletracker.utils.toCampaignSensorName
import kaist.iclab.tracker.permission.AndroidPermissionManager
import kaist.iclab.tracker.permission.PermissionState
import kaist.iclab.tracker.sensor.controller.BackgroundController
import kaist.iclab.tracker.sensor.controller.ControllerState
import kaist.iclab.tracker.sensor.core.Sensor
import kaist.iclab.tracker.sensor.core.SensorState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the Settings screen.
 *
 * Manages sensor toggle states, permission requests, and data collection lifecycle.
 * Observes background controller state changes and automatically starts/stops the
 * PhoneSensorDataService accordingly.
 *
 * Key responsibilities:
 * - Toggle individual sensors on/off
 * - Request and observe permissions
 * - Start/stop data logging
 * - Manage AutoSyncService lifecycle
 *
 * @param backgroundController Controller managing sensor background collection
 * @param permissionManager Manager for Android permission requests
 * @param syncTimestampService Service for tracking sync timestamps
 * @param context Application context
 */
class SettingsViewModel(
    private val backgroundController: BackgroundController,
    private val permissionManager: AndroidPermissionManager,
    private val syncTimestampService: SyncTimestampService,
    private val campaignSensorRepository: CampaignSensorRepository,
    private val context: Context
) : ViewModel() {
    companion object {
        private const val TAG = "SettingsViewModel"
    }

    private val sensors = backgroundController.sensors

    val sensorMap = sensors.associateBy { it.name }

    // Reactive filtered sensor state based on active campaign sensors
    val sensorState: StateFlow<Map<String, StateFlow<SensorState>>> =
        campaignSensorRepository.activeSensorsFlow
            .combine(MutableStateFlow(sensors)) { activeSensors, allSensors ->
                val activeNames = activeSensors.map { it.name }
                allSensors.filter { sensor: Sensor<*, *> ->
                    val campaignSensorName = sensor.id.toCampaignSensorName()
                    activeNames.contains(campaignSensorName)
                }.associate { sensor: Sensor<*, *> -> sensor.name to sensor.sensorStateFlow }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = sensors.associate { it.name to it.sensorStateFlow }
            )
    val controllerState = backgroundController.controllerStateFlow

    init {
        // Note: PhoneSensorDataService lifecycle is managed at the Application level
        // (MobileTrackerApplication) to survive ViewModel recreation.
    }


    /**
     * Toggles a sensor on or off based on its current state
     */
    fun toggleSensor(sensorName: String) {
        val sensor = getSensor(sensorName) ?: return
        val currentState = sensorState.value[sensorName]?.value?.flag ?: return

        when (currentState) {
            SensorState.FLAG.DISABLED -> enableSensor(sensor, sensorName)
            SensorState.FLAG.ENABLED -> disableSensor(sensor, sensorName)
            else -> Unit
        }
    }

    /**
     * Gets a sensor by name, logging an error if not found
     */
    private fun getSensor(sensorName: String): Sensor<*, *>? {
        return sensorMap[sensorName] ?: run {
            Log.e(TAG, "Sensor not found: $sensorName")
            null
        }
    }

    /**
     * Enables a sensor by requesting permissions and observing the permission flow
     */
    private fun enableSensor(sensor: Sensor<*, *>, sensorName: String) {
        permissionManager.request(sensor.permissions)
        observePermissionFlow(
            permissions = sensor.permissions,
            onGranted = { sensor.enable() },
            errorContext = "enabling sensor $sensorName"
        )
    }

    /**
     * Disables a sensor
     */
    private fun disableSensor(sensor: Sensor<*, *>, sensorName: String) {
        try {
            sensor.disable()
        } catch (e: Exception) {
            Log.e(TAG, "Error disabling sensor $sensorName: ${e.message}", e)
        }
    }

    /**
     * Observes permission flow and executes callback when all permissions are granted
     */
    private fun observePermissionFlow(
        permissions: Array<String>,
        onGranted: () -> Unit,
        errorContext: String
    ) {
        var job: Job? = null
        job = viewModelScope.launch {
            try {
                permissionManager.getPermissionFlow(permissions)
                    .collect { permissionMap ->
                        if (permissionMap.values.all { it == PermissionState.GRANTED }) {
                            try {
                                onGranted()
                            } catch (e: Exception) {
                                Log.e(
                                    TAG,
                                    "Error in onGranted callback for $errorContext: ${e.message}",
                                    e
                                )
                            }
                            job?.cancel()
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error observing permission flow for $errorContext: ${e.message}", e)
                job?.cancel()
            }
        }
    }

    /**
     * Checks if notification permission is granted
     */
    fun hasNotificationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Requests notification permission and automatically starts logging when granted.
     * For Android versions below 13, starts logging directly as permission is not required.
     */
    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionManager.request(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
            observeNotificationPermissionFlow()
        } else {
            // Android versions below 13 don't require notification permission
            startLoggingSafely()
        }
    }

    /**
     * Observes notification permission flow and starts logging when granted
     */
    private fun observeNotificationPermissionFlow() {
        observePermissionFlow(
            permissions = arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            onGranted = {
                startLoggingSafely()
                Log.d(TAG, "Started logging after notification permission granted")
            },
            errorContext = "notification permission"
        )
    }

    /**
     * Starts logging with proper error handling
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun startLogging() {
        startLoggingSafely()
    }

    /**
     * Starts logging with error handling (internal safe method)
     */
    private fun startLoggingSafely() {
        try {
            backgroundController.start()
            // Track when data collection starts
            syncTimestampService.updateDataCollectionStarted()
            // Start auto-sync service
            AutoSyncService.start(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting logging: ${e.message}", e)
        }
    }

    /**
     * Stops logging with proper error handling
     */
    fun stopLogging() {
        try {
            backgroundController.stop()
            // Clear data collection started timestamp
            syncTimestampService.clearDataCollectionStarted()
            // Stop auto-sync service
            AutoSyncService.stop(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping logging: ${e.message}", e)
        }
    }
}

