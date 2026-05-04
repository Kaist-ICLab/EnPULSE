package kaist.iclab.tracker.trigger.state

import kaist.iclab.tracker.trigger.model.DetectionState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Maintains the latest detection label for each sensor type.
 *
 * Sensors (via [SensorDetectionAdapter]s) call [updateState] whenever they produce
 * a new detection. The trigger engine observes [stateChanges] to know when to
 * re-evaluate condition trees.
 *
 * This class is **source-agnostic** — states can come from local sensors, remote
 * BLE updates, or test harnesses. This enables future cross-device trigger evaluation
 * without any changes to this class.
 *
 * Thread-safe: uses [ConcurrentHashMap] for state storage.
 */
class DetectionStateTracker {

    private val states = ConcurrentHashMap<String, DetectionState>()

    private val _stateChanges = MutableSharedFlow<Pair<String, DetectionState>>(
        extraBufferCapacity = 16
    )

    /**
     * Flow of state change events. Emits only when a sensor's detection value
     * actually changes (not on duplicate updates with the same value).
     */
    val stateChanges: SharedFlow<Pair<String, DetectionState>> = _stateChanges

    /**
     * Update the detection state for a sensor.
     *
     * If the new value differs from the current value, a change event is emitted
     * on [stateChanges]. Duplicate updates with the same value are silently ignored
     * to avoid unnecessary trigger evaluations.
     *
     * @param sensor The sensor name (e.g., "stress", "physical_activity").
     * @param value The detected label (e.g., "High", "Drinking").
     * @param timestamp Epoch milliseconds of the detection.
     */
    fun updateState(sensor: String, value: String, timestamp: Long) {
        val newState = DetectionState(value, timestamp)
        val oldState = states.put(sensor, newState)
        if (oldState?.value != value) {
            _stateChanges.tryEmit(sensor to newState)
        }
    }

    /**
     * Get the latest detection state for a specific sensor.
     * @return The detection state, or null if no detection has been reported.
     */
    fun getState(sensor: String): DetectionState? = states[sensor]

    /**
     * Get a snapshot of all current detection states.
     * @return An immutable copy of the state map.
     */
    fun getAllStates(): Map<String, DetectionState> = states.toMap()

    /**
     * Clear all tracked states. Useful when triggers are reloaded or the engine restarts.
     */
    fun clear() {
        states.clear()
    }
}
