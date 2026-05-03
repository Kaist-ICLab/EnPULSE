package kaist.iclab.mobiletracker.data.survey

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Supabase entity for the 'survey' table
 */
@Serializable
data class SurveyEntity(
    val id: Int,
    @SerialName("campaign_id") val campaignId: Int,
    @SerialName("schedule_method") val scheduleMethod: JsonObject? = null,
    val title: String,
    val description: String? = null,
    @SerialName("device_type") val deviceType: Int? = 0,               // 0 = phone, 1 = watch
    @SerialName("expire_after_ms") val expireAfterMs: Long? = null      // auto-dismiss timer (ms), null = no expiry
)

/**
 * Supabase entity for the 'survey_question' table
 */
@Serializable
data class SurveyQuestionEntity(
    val id: Int,
    @SerialName("survey_id") val surveyId: Int,
    val question: String,
    @SerialName("is_mandatory") val isMandatory: Boolean,
    @SerialName("answer_type") val answerType: String,
    val config: JsonObject? = null,
    @SerialName("triggered_by") val triggeredBy: Int? = null
)

/**
 * Supabase entity for the 'survey_question_trigger' table
 */
@Serializable
data class SurveyQuestionTriggerEntity(
    val id: Int,
    @SerialName("question_id") val questionId: Int,
    val expression: JsonObject
)

/**
 * Entity for inserting survey question responses into Supabase
 * Maps to the 'survey_question_response' table
 */
@Serializable
data class SurveyQuestionResponseInsert(
    @SerialName("question_id") val questionId: Int,
    val uuid: String,
    @SerialName("trigger_time") val triggerTime: String,
    @SerialName("actual_trigger_time") val actualTriggerTime: String,
    @SerialName("survey_start_time") val surveyStartTime: String,
    @SerialName("response_submission_time") val responseSubmissionTime: String,
    val response: JsonElement
)
