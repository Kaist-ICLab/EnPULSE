package kaist.iclab.tracker.trigger.adapter.galaxywatch

import kaist.iclab.tracker.sensor.galaxywatch.GestureSensor
import kaist.iclab.tracker.trigger.adapter.SensorDetectionAdapter
import kaist.iclab.tracker.trigger.state.DetectionStateTracker

/**
 * Bridges [GestureSensor] output to the trigger engine's detection state model.
 *
 * Listens to gesture classification results and maps `classIndex` to the
 * corresponding label from [GestureSensor.LABELS] (27 gesture classes).
 *
 * Available labels:
 * "Alarm Clock", "Blender In Use", "Brushing Hair", "Chopping", "Clapping",
 * "Coughing", "Drill In Use", "Drinking", "Grating", "Hair Dryer In Use",
 * "Hammering", "Knocking", "Laughing", "Microwave", "Pouring Pitcher",
 * "Sanding", "Scratching", "Screwing", "Shaver In Use", "Toilet Flushing",
 * "Toothbrushing", "Twisting Jar", "Vacuum In Use", "Washing Utensils",
 * "Washing Hands", "Wiping With Rag", "Other"
 *
 * Condition JSON usage:
 * ```json
 * { "type": "detection", "sensor": "physical_activity", "value": "Drinking" }
 * ```
 */
class GestureDetectionAdapter(
    private val gestureSensor: GestureSensor,
    private val tracker: DetectionStateTracker
) : SensorDetectionAdapter {

    override val sensorName: String = "physical_activity"

    private val listener: (GestureSensor.Entity) -> Unit = { entity ->
        val label = GestureSensor.LABELS.getOrElse(entity.classIndex) { "Other" }
        tracker.updateState(sensorName, label, entity.timestamp)
    }

    override fun start() {
        gestureSensor.addListener(listener)
    }

    override fun stop() {
        gestureSensor.removeListener(listener)
    }
}
