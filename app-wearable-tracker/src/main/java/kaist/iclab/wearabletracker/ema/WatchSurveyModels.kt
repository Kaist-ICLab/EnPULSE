package kaist.iclab.wearabletracker.ema

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Watch-side domain models for microEMA surveys.
 * Maps to the existing Supabase schema (survey, survey_question, survey_question_option tables).
 */

/**
 * A single option for a survey question.
 * Maps to `survey_question_option` table rows.
 */
@Serializable
data class WatchOption(
    val id: Int,
    val display: String
)

/**
 * Answer type for a question. Derived from `survey_question.answer_type`.
 * The watch UI determines the input widget based on this type + number of options:
 *   - RADIO with ≤2 options → two large tap buttons (Yes/No style)
 *   - RADIO with 3+ options → horizontal carousel or scrollable chips (Likert/Categorical)
 *   - CHECKBOX → scrollable list of toggle chips (multi-select)
 *   - NUMBER → crown-scrollable picker 0–10
 *   - TEXT → manual keyboard/handwriting entry or direct STT voice dictation
 */
@Serializable
enum class AnswerType {
    @SerialName("RADIO") RADIO,
    @SerialName("CHECKBOX") CHECKBOX,
    @SerialName("NUMBER") NUMBER,
    @SerialName("TEXT") TEXT
}

/**
 * A single question within a microEMA survey.
 * Maps to `survey_question` + associated `survey_question_option` rows.
 */
@Serializable
data class WatchQuestion(
    val id: Int,
    @SerialName("surveyId") val surveyId: Int,
    val text: String,
    @SerialName("answerType") val answerType: AnswerType,
    @SerialName("isMandatory") val isMandatory: Boolean,
    val options: List<WatchOption> = emptyList()
)

/**
 * Complete survey configuration for the watch.
 * Loaded from bundled JSON asset (Phase 1) or fetched from phone via MessageClient (later phase).
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
 * Required for research validity (per R&D plan Section 6).
 */
enum class ResponseStatus {
    ANSWERED,
    EXPIRED,
    DISMISSED
}

/**
 * A single question's response within a microEMA session.
 */
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
