package kaist.iclab.mobiletracker.data.survey

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Compatibility models for MicroEMA (matching watch structure)
 */

@Serializable
data class WatchSurveyConfig(
    val surveyId: Int,
    val title: String,
    @SerialName("expireAfterMs") val expireAfterMs: Long? = null,
    val questions: List<WatchQuestion>
)

@Serializable
data class WatchQuestion(
    val id: Int,
    val surveyId: Int,
    val text: String,
    val answerType: AnswerType,
    val isMandatory: Boolean = true,
    val options: List<WatchOption>? = null
)

@Serializable
data class WatchOption(
    val id: Int,
    val display: String
)

@Serializable
enum class AnswerType {
    RADIO,
    CHECKBOX,
    TEXT,
    NUMBER
}

enum class ResponseStatus {
    ANSWERED,
    EXPIRED,
    DISMISSED
}

data class MicroEmaResponse(
    val surveyId: Int,
    val questionId: Int,
    val answer: String?,
    val status: ResponseStatus,
    val triggerTime: Long,
    val surveyStartTime: Long,
    val responseTime: Long?
)
