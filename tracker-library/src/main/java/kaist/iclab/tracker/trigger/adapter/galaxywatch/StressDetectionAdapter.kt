package kaist.iclab.tracker.trigger.adapter.galaxywatch

import kaist.iclab.tracker.sensor.galaxywatch.StressSensor
import kaist.iclab.tracker.trigger.adapter.SensorDetectionAdapter
import kaist.iclab.tracker.trigger.state.DetectionStateTracker

/**
 * Bridges [StressSensor] output to the trigger engine's detection state model.
 *
 * Listens to stress inference results and maps them to detection labels:
 * - `isHighStress = true`  → `"High"`
 * - `isHighStress = false` → `"Low"`
 *
 * Condition JSON usage:
 * ```json
 * { "type": "detection", "sensor": "stress", "value": "High" }
 * ```
 */
class StressDetectionAdapter(
    private val stressSensor: StressSensor,
    private val tracker: DetectionStateTracker
) : SensorDetectionAdapter {

    override val sensorName: String = "stress"

    private val listener: (StressSensor.Entity) -> Unit = { entity ->
        val label = if (entity.isHighStress) "High" else "Low"
        tracker.updateState(sensorName, label, entity.timestamp)
    }

    override fun start() {
        stressSensor.addListener(listener)
    }

    override fun stop() {
        stressSensor.removeListener(listener)
    }
}
