package kaist.iclab.tracker.trigger

import kotlinx.serialization.Serializable

/**
 * Represents a complete rule for triggering an action based on sensor data.
 */
@Serializable
data class TriggerRule(
    val id: String,
    val sourceSensorId: String,
    val window: WindowConfig,
    val calculation: CalculationConfig,
    val condition: ConditionConfig,
    val action: ActionConfig
)

@Serializable
data class WindowConfig(
    val type: String, // e.g., "TUMBLING", "SLIDING"
    val sizeMs: Long,
    val stepMs: Long? = null
)

@Serializable
data class CalculationConfig(
    val type: String, // e.g., "MEAN", "MAX", "MIN", "VARIANCE"
    val dataField: String // Which field of the JSON payload to calculate on
)

@Serializable
data class ConditionConfig(
    val operator: String, // e.g., "GREATER_THAN", "LESS_THAN", "EQUALS"
    val threshold: Double
)

@Serializable
data class ActionConfig(
    val type: String, // e.g., "MICRO_EMA"
    val payload: String? = null // Optional extra data (e.g., survey ID)
)
