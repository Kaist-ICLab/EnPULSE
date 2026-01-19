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
import kaist.iclab.mobiletracker.services.AutoSyncService
import kaist.iclab.mobiletracker.services.PhoneSensorDataService
import kaist.iclab.mobiletracker.services.SyncTimestampService
import kaist.iclab.tracker.permission.AndroidPermissionManager
import kaist.iclab.tracker.permission.PermissionState
import kaist.iclab.tracker.sensor.controller.BackgroundController
import kaist.iclab.tracker.sensor.controller.ControllerState
import kaist.iclab.tracker.sensor.core.Sensor
import kaist.iclab.tracker.sensor.core.SensorState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val backgroundController: BackgroundController,
    private val permissionManager: AndroidPermissionManager,
    private val syncTimestampService: SyncTimestampService,
    private val context: Context
) : ViewModel() {
    companion object {
        private const val TAG = "SettingsViewModel"
    }

    private val sensors = backgroundController.sensors

    val sensorMap = sensors.associateBy { it.name }
    val sensorState = sensors.associate { it.name to it.sensorStateFlow }
    val controllerState = backgroundController.controllerStateFlow

    init {
        observeControllerState()
    }

    /**
     * Observes controller state changes and manages PhoneSensorDataService lifecycle
     */
    private fun observeControllerState() {
        viewModelScope.launch(Dispatchers.IO) {
            backgroundController.controllerStateFlow
                .catch { e ->
                    Log.e(TAG, "Error observing controller state: ${e.message}", e)
                }
                .collect { state ->
                    handleControllerStateChange(state)
                }
        }
    }

    /**
     * Handles controller state changes by starting/stopping the phone sensor service
     */
    private fun handleControllerStateChange(state: ControllerState) {
        try {
            when (state.flag) {
                ControllerState.FLAG.RUNNING -> PhoneSensorDataService.start(context)
                else -> PhoneSensorDataService.stop(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error managing phone sensor service: ${e.message}", e)
        }
    }

    /**
     * Toggles a sensor on or off based on its current state
     */
    fun toggleSensor(sensorName: String) {
        val sensor = getSensor(sensorName) ?: return
        val currentState = sensorState[sensorName]?.value?.flag ?: return

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
            permissionManager.getPermissionFlow(permissions)
                .catch { e ->
                    Log.e(TAG, "Error observing permission flow for $errorContext: ${e.message}", e)
                    job?.cancel()
                }
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

