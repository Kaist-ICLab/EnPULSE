package kaist.iclab.tracker.trigger.model

import kotlinx.serialization.Serializable

/**
 * Represents the latest detection output from a sensor.
 *
 * @param value The detected label (e.g., "High", "Low", "Drinking", "Still").
 * @param timestamp The timestamp (epoch ms) when the detection was produced.
 */
@Serializable
data class DetectionState(
    val value: String,
    val timestamp: Long
)
