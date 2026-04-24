package kaist.iclab.tracker.sensor.microema

import kaist.iclab.tracker.sensor.survey.config.SurveyConfig

/**
 * Builder for MicroEMA-specific configurations.
 */
object MicroEmaBuilder {

    /**
     * Builds a [WatchSurveyConfig] from a unified [SurveyConfig].
     */
    fun build(config: SurveyConfig): WatchSurveyConfig {
        return WatchSurveyConfig(
            surveyId = config.id,
            title = config.title,
            expireAfterMs = config.expireAfterMs,
            questions = config.questions.map { q ->
                WatchQuestion(
                    id = q.id,
                    surveyId = config.id,
                    text = q.text,
                    answerType = try {
                        AnswerType.valueOf(q.type.uppercase())
                    } catch (e: Exception) {
                        AnswerType.TEXT
                    },
                    isMandatory = q.isMandatory,
                    options = q.options?.map { opt ->
                        WatchOption(id = opt.id, display = opt.display)
                    } ?: emptyList()
                )
            }
        )
    }
}
