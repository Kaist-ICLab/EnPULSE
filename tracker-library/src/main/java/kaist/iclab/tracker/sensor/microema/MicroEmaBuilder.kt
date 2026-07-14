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
            description = config.description,
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
                    options = buildOptions(q)
                )
            }
        )
    }

    private fun buildOptions(question: kaist.iclab.tracker.sensor.survey.config.QuestionConfig): List<WatchOption> {
        return when (question.type.uppercase()) {
            "BINARY" -> listOf(
                WatchOption(id = 0, display = "Yes"),
                WatchOption(id = 1, display = "No")
            )

            "NUMBERSCALE" -> {
                val min = question.min ?: 0
                val max = question.max ?: 10
                val values = if (min <= max) min..max else max..min
                values.mapIndexed { index, value ->
                    WatchOption(id = index, display = value.toString())
                }
            }

            else -> question.options?.mapIndexed { index, opt ->
                WatchOption(id = index, display = opt)
            } ?: emptyList()
        }
    }
}
