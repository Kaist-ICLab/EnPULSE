package kaist.iclab.tracker.trigger.adapter

import kaist.iclab.tracker.sensor.phone.TimingSensor
import kaist.iclab.tracker.trigger.state.DetectionStateTracker

/**
 * Bridges [TimingSensor] output to the trigger engine's detection state model.
 *
 * Mirrors [kaist.iclab.tracker.trigger.adapter.galaxywatch.StressDetectionAdapter] exactly —
 * not placed under the `galaxywatch` subpackage since [TimingSensor] is platform-agnostic
 * (phone-only in practice, since the trigger engine lives on the phone).
 *
 * Unlike a continuous reading (stress is always "High" or "Low"), a timing fire is a discrete
 * instant: the detection is pushed as the fired [TimingSensor.Entity.value], then immediately
 * reset to `"idle"` so the condition is only true for the evaluation cycle right around the
 * fire, not "stuck true" until some unrelated state change happens to occur later.
 *
 * Condition JSON usage:
 * ```json
 * { "type": "detection", "sensor": "timing", "value": "morning_window" }
 * ```
 */
class TimingDetectionAdapter(
    private val timingSensor: TimingSensor,
    private val tracker: DetectionStateTracker
) : SensorDetectionAdapter {

    companion object {
        const val IDLE = "idle"
    }

    override val sensorName: String = "timing"

    private val listener: (TimingSensor.Entity) -> Unit = { entity ->
        tracker.updateState(sensorName, entity.value, entity.timestamp)
        tracker.updateState(sensorName, IDLE, entity.timestamp)
    }

    override fun start() {
        timingSensor.addListener(listener)
    }

    override fun stop() {
        timingSensor.removeListener(listener)
    }
}
