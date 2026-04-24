package kaist.iclab.tracker.ema

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Shared domain models for microEMA surveys.
 * Used by both phone and watch modules to ensure consistent serialization.
 */

/**
 * A single option for a survey question.
 */
@Serializable
data class WatchOption(
    val id: Int,
    val display: String
)

/**
 * Answer type for a question.
 */
@Serializable
enum class AnswerType {
    @SerialName("RADIO")
    RADIO,

    @SerialName("CHECKBOX")
    CHECKBOX,

    @SerialName("NUMBER")
    NUMBER,

    @SerialName("TEXT")
    TEXT
}

/**
 * A single question within a microEMA survey.
 */
@Serializable
data class WatchQuestion(
    val id: Int,
    @SerialName("surveyId") val surveyId: Int,
    val text: String,
    @SerialName("answerType") val answerType: AnswerType,
    @SerialName("isMandatory") val isMandatory: Boolean = true,
    val options: List<WatchOption> = emptyList()
)

/**
 * Complete survey configuration.
 */
@Serializable
data class WatchSurveyConfig(
    @SerialName("surveyId") val surveyId: Int,
    val title: String,
    @SerialName("expireAfterMs") val expireAfterMs: Long? = null,
    val questions: List<WatchQuestion>
)

/**
 * Tracks the compliance status of each question response.
 */
@Serializable
enum class ResponseStatus {
    ANSWERED,
    EXPIRED,
    DISMISSED
}

/**
 * A single question's response within a microEMA session.
 */
@Serializable
data class MicroEmaResponse(
    val id: Long? = null,
    val surveyId: Int,
    val questionId: Int,
    val answer: String?,
    val status: ResponseStatus,
    val triggerTime: Long,
    val surveyStartTime: Long,
    val responseTime: Long?
)
