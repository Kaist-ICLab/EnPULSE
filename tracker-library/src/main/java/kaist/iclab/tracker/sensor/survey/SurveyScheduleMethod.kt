package kaist.iclab.tracker.sensor.survey

sealed class SurveyScheduleMethod {
    data class ESM(
        val minInterval: Long,
        val maxInterval: Long,
        val numSurvey: Int,
    ) : SurveyScheduleMethod()

    data class Fixed(
        val timeOfDay: List<Long>
    ) : SurveyScheduleMethod()
}