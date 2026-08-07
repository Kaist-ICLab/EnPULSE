package kaist.iclab.tracker.sensor.timing

/**
 * A single computed/persisted fire instant for one [kaist.iclab.tracker.sensor.phone.TimingSensor.TimingScheduleEntry].
 *
 * Mirrors [kaist.iclab.tracker.sensor.survey.SurveySchedule]'s shape, but tracks schedules
 * keyed by the entry's [value] label instead of a survey id, and only needs a single
 * [firedAt] marker (TimingSensor has no per-response bookkeeping to do — that stays the
 * job of [kaist.iclab.tracker.storage.core.SurveyScheduleStorage], used by SurveySensor).
 */
data class TimingSchedule(
    val scheduleId: String? = null,
    val value: String,
    val triggerTime: Long? = null,
    val firedAt: Long? = null,
)
