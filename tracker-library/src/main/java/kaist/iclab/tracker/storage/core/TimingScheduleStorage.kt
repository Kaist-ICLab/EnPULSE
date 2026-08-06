package kaist.iclab.tracker.storage.core

import kaist.iclab.tracker.sensor.timing.TimingSchedule

/**
 * Persists computed fire instants for [kaist.iclab.tracker.sensor.phone.TimingSensor].
 *
 * Mirrors [SurveyScheduleStorage]'s query shape (next/last un-fired schedule, optionally
 * scoped to one key), but keyed by a schedule entry's `value` label instead of a `surveyId`,
 * and only tracks a single `firedAt` marker rather than the full response-lifecycle timestamps
 * SurveySensor needs.
 */
interface TimingScheduleStorage {
    fun getNextSchedule(value: String? = null): TimingSchedule?
    fun getLastSchedule(value: String? = null): TimingSchedule?
    fun addSchedule(schedule: TimingSchedule): String

    fun setFiredAt(scheduleId: String, timestamp: Long)

    fun resetSchedule()
}
