package kaist.iclab.tracker.sensor.survey

data class SurveySchedule(
    val scheduleId: String? = null,
    val surveyId: String,
    val triggerTime: Long? = null,
    val actualTriggerTime: Long? = null,
    val surveyStartTime: Long? = null,
    val responseSubmissionTime: Long? = null,
)