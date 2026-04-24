package kaist.iclab.tracker.sensor.microema

import kaist.iclab.tracker.sensor.survey.config.QuestionConfig
import kaist.iclab.tracker.sensor.survey.config.SurveyConfig

/**
 * Utility to convert generic SurveyConfig into watch-specific WatchSurveyConfig.
 */
object MicroEmaBuilder {

    /**
     * Builds a WatchSurveyConfig from a generic SurveyConfig.
     */
    fun build(config: SurveyConfig): WatchSurveyConfig {
        return WatchSurveyConfig(
            surveyId = config.id.toIntOrNull() ?: 0,
            title = config.notification.title,
            expireAfterMs = null, // Can be extended to read from config if added to SurveyConfig
            questions = config.questions.map { mapQuestion(it, config.id.toIntOrNull() ?: 0) }
        )
    }

    private fun mapQuestion(config: QuestionConfig, surveyId: Int): WatchQuestion {
        val answerType = try {
            AnswerType.valueOf(config.type.uppercase())
        } catch (e: Exception) {
            AnswerType.TEXT // Fallback
        }

        return WatchQuestion(
            id = config.id,
            surveyId = surveyId,
            text = config.text,
            answerType = answerType,
            isMandatory = config.isMandatory,
            options = config.options?.mapIndexed { index, option ->
                WatchOption(id = index, display = option.display)
            } ?: emptyList()
        )
    }
}
