package kaist.iclab.tracker.sensor.survey.config

import kotlinx.serialization.Serializable

/**
 * Unified Survey configuration domain model.
 * Used across phone (EMA) and watch (MicroEMA) platforms.
 */
@Serializable
data class SurveyConfig(
    val id: Int,
    val campaignId: Int,
    val title: String,
    val description: String?,
    val scheduleType: String,
    val schedule: String?,  // JSON string representing the schedule configuration
    val deviceType: Int = 0, // 0 = phone, 1 = watch
    val expireAfterMs: Long? = null,
    val questions: List<QuestionConfig>
)

/**
 * Wrapper for List<SurveyConfig> for persistence.
 */
@Serializable
data class SurveyConfigList(
    val configs: List<SurveyConfig> = emptyList()
)

/**
 * Unified Question configuration.
 * Uses a flat structure with parentId for conditional branching.
 */
@Serializable
data class QuestionConfig(
    val id: Int,
    val parentId: Int? = null,
    val type: String,
    val text: String,
    val isMandatory: Boolean,
    val trigger: String? = null,  // JSON string representing the expression to show this question
    val options: List<OptionConfig>? = null
)

/**
 * Unified Option configuration for RADIO/CHECKBOX questions.
 */
@Serializable
data class OptionConfig(
    val id: Int,
    val display: String,
    val allowFreeResponse: Boolean = false
)

/**
 * Schedule types for surveys
 */
object ScheduleType {
    const val MANUAL = "MANUAL"
    const val TIME_OF_DAY = "TIME_OF_DAY"
    const val ESM = "ESM"
}
