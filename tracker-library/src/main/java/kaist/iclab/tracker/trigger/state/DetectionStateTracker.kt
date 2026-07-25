package kaist.iclab.tracker.trigger.state

import kaist.iclab.tracker.trigger.model.DetectionState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.channels.BufferOverflow
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
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /**
     * Flow of state change events. Emits every time a sensor produces a new detection,
     * regardless of whether the value changed.
     */
    val stateChanges: SharedFlow<Pair<String, DetectionState>> = _stateChanges

    /**
     * Update the detection state for a sensor.
     *
     * This emits a change event on [stateChanges] on EVERY update.
     * Duplicate updates with the same value are NOT ignored so that
     * continuous triggers (e.g., repeating while "Still") work correctly.
     * Trigger throttling is handled by the TriggerEngine.
     *
     * @param sensor The sensor name (e.g., "stress", "physical_activity").
     * @param value The detected label (e.g., "High", "Drinking").
     * @param timestamp Epoch milliseconds of the detection.
     */
    fun updateState(sensor: String, value: String, timestamp: Long) {
        val newState = DetectionState(value, timestamp)
        states[sensor] = newState
        _stateChanges.tryEmit(sensor to newState)
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
