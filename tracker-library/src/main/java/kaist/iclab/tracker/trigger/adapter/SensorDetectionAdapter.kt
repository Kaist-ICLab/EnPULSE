package kaist.iclab.tracker.trigger.adapter

/**
 * Bridge between a sensor's raw output and the trigger engine's detection state model.
 *
 * Implementations listen to a specific sensor and convert its entities into
 * string detection labels, which are then fed into a [DetectionStateTracker].
 *
 * Each adapter is responsible for one sensor type and produces a single detection
 * label per update (e.g., "High"/"Low" for stress, "Drinking"/"Other" for gesture).
 *
 * To add support for a new sensor in condition trees:
 * 1. Create a new adapter implementing this interface.
 * 2. Set [sensorName] to match the `"sensor"` field used in the condition JSON.
 * 3. In [start], attach a listener to the sensor and call
 *    [DetectionStateTracker.updateState] with the appropriate label.
 * 4. Register the adapter in the DI module.
 */
interface SensorDetectionAdapter {

    /**
     * The sensor name used in condition JSON (e.g., "stress", "physical_activity").
     * Must match the `"sensor"` field in [ConditionNode.Detection].
     */
    val sensorName: String

    /**
     * Start listening to the sensor and updating the detection state tracker.
     * Idempotent — calling start() multiple times should be safe.
     */
    fun start()

    /**
     * Stop listening to the sensor.
     * Idempotent — calling stop() multiple times should be safe.
     */
    fun stop()
}
