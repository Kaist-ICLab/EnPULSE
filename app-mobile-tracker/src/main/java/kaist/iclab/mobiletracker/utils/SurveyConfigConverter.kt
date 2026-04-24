package kaist.iclab.mobiletracker.utils

import kaist.iclab.tracker.sensor.survey.SurveySensor
import kaist.iclab.tracker.sensor.survey.config.SurveyBuilder
import kaist.iclab.tracker.sensor.survey.config.SurveyConfig

/**
 * Utility to convert unified SurveyConfig domain models to runtime Survey objects.
 */
object SurveyConfigConverter {

    /**
     * Convert a list of SurveyConfig to SurveySensor.Config.
     */
    fun toSurveySensorConfig(configs: List<SurveyConfig>): SurveySensor.Config {
        val surveys = configs.associate {
            val survey = SurveyBuilder.build(it)
            survey.id to survey
        }
        return SurveySensor.Config(
            survey = surveys
        )
    }
}
