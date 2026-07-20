package kaist.iclab.wearabletracker.ecg

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kaist.iclab.tracker.permission.PermissionManager
import kaist.iclab.tracker.permission.PermissionState
import kaist.iclab.tracker.sensor.core.SensorState
import kaist.iclab.tracker.sensor.galaxywatch.ECGSensor
import kaist.iclab.wearabletracker.db.obx.WatchSensorStores
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class EcgMeasurementUiState {
    data object Checking : EcgMeasurementUiState()
    data object Unavailable : EcgMeasurementUiState()
    data object NeedsPermission : EcgMeasurementUiState()
    data class Running(val remainingMs: Long, val totalMs: Long = EcgViewModel.MEASUREMENT_DURATION_MS) :
        EcgMeasurementUiState()

    data object Completed : EcgMeasurementUiState()
}

/**
 * Owns the lifecycle of a single 30s on-demand ECG measurement.
 *
 * Unlike every other watch sensor, [ECGSensor] is intentionally excluded from
 * [kaist.iclab.tracker.sensor.controller.BackgroundController]'s managed sensor list
 * (see KoinModule.kt), so nothing else calls init()/enable()/start()/stop() on it —
 * this ViewModel drives its full lifecycle directly: init once, enable, listen +
 * persist emitted entities, start, run a 30s countdown, then stop/disable/cleanup.
 */
class EcgViewModel(
    private val ecgSensor: ECGSensor,
    private val watchSensorStores: WatchSensorStores,
    private val permissionManager: PermissionManager
) : ViewModel() {

    companion object {
        private val TAG = EcgViewModel::class.simpleName
        private const val COUNTDOWN_TICK_MS = 1000L
        const val MEASUREMENT_DURATION_MS = 30_000L
    }

    private val _uiState = MutableStateFlow<EcgMeasurementUiState>(EcgMeasurementUiState.Checking)
    val uiState: StateFlow<EcgMeasurementUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null
    private var isRunning = false

    private val dataListener: (ECGSensor.Entity) -> Unit = { entity ->
        watchSensorStores.ecg.insertFromSensorEntities(listOf(entity))
    }

    init {
        // ECG isn't in BackgroundController's managed list, so nobody else calls init() for it.
        ecgSensor.init()
    }

    /**
     * Starts the 30s measurement. Call once when the measurement screen is first composed.
     * Permission is expected to already be requested by the caller (SettingsScreen) before
     * this activity was launched — this only verifies the grant, it does not request it.
     */
    fun startMeasurement() {
        if (isRunning || _uiState.value is EcgMeasurementUiState.Running) return

        if (ecgSensor.sensorStateFlow.value.flag == SensorState.FLAG.UNAVAILABLE) {
            _uiState.value = EcgMeasurementUiState.Unavailable
            return
        }

        val permissionStates = permissionManager.getPermissionFlow(ecgSensor.permissions).value
        val allGranted = ecgSensor.permissions.all { permissionStates[it] == PermissionState.GRANTED }
        if (!allGranted) {
            _uiState.value = EcgMeasurementUiState.NeedsPermission
            return
        }

        ecgSensor.enable()
        if (ecgSensor.sensorStateFlow.value.flag != SensorState.FLAG.ENABLED) {
            _uiState.value = EcgMeasurementUiState.NeedsPermission
            return
        }

        ecgSensor.addListener(dataListener)
        ecgSensor.start()
        isRunning = true
        startCountdown()
    }

    private fun startCountdown() {
        countdownJob = viewModelScope.launch {
            var remaining = MEASUREMENT_DURATION_MS
            while (remaining > 0) {
                _uiState.value = EcgMeasurementUiState.Running(remaining)
                delay(COUNTDOWN_TICK_MS)
                remaining -= COUNTDOWN_TICK_MS
            }
            finishMeasurement()
        }
    }

    /** Stop early, e.g. on back press — skips the completion screen. */
    fun cancelMeasurement() {
        stopSensor()
    }

    private fun finishMeasurement() {
        stopSensor()
        _uiState.value = EcgMeasurementUiState.Completed
    }

    private fun stopSensor() {
        countdownJob?.cancel()
        countdownJob = null
        if (isRunning) {
            try {
                ecgSensor.stop()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to stop ECG sensor", e)
            }
            ecgSensor.removeListener(dataListener)
            ecgSensor.disable()
            isRunning = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopSensor()
    }
}
