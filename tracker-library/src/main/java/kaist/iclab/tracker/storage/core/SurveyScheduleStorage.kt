package kaist.iclab.tracker.storage.core

import kaist.iclab.tracker.sensor.survey.SurveySchedule

interface SurveyScheduleStorage {
    fun getNextSchedule(): SurveySchedule?
    fun getLastSchedule(): SurveySchedule?
    fun getScheduleByScheduleId(scheduleId: String): SurveySchedule?
    fun addSchedule(schedule: SurveySchedule): String

    fun setActualTriggerTime(scheduleId: String, timestamp: Long)
    fun setSurveyStartTime(scheduleId: String, timestamp: Long)
    fun setResponseSubmissionTime(scheduleId: String, timestamp: Long)

    fun resetSchedule()
}