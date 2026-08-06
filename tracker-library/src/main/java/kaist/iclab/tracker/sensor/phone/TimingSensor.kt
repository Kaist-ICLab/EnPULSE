package kaist.iclab.tracker.sensor.phone

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import kaist.iclab.tracker.R
import kaist.iclab.tracker.TrackerUtil.formatLocalDateTime
import kaist.iclab.tracker.listener.AlarmListener
import kaist.iclab.tracker.listener.SingleAlarmListener
import kaist.iclab.tracker.permission.PermissionManager
import kaist.iclab.tracker.sensor.core.BaseSensor
import kaist.iclab.tracker.sensor.core.SensorConfig
import kaist.iclab.tracker.sensor.core.SensorEntity
import kaist.iclab.tracker.sensor.core.SensorState
import kaist.iclab.tracker.sensor.survey.SurveyScheduleMethod
import kaist.iclab.tracker.sensor.timing.TimingSchedule
import kaist.iclab.tracker.storage.core.StateStorage
import kaist.iclab.tracker.storage.core.TimingScheduleStorage
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import kotlin.math.pow

/**
 * Fires a named [Entity] whenever one of its configured [Config.schedules] entries comes due,
 * using the exact same ESM/fixed-time-of-day scheduling algorithm [SurveySensor] used to own
 * directly. Architected identically to any other [BaseSensor] (e.g. galaxywatch's StressSensor) —
 * it does not evaluate trigger conditions or know about surveys at all. A companion
 * `TimingDetectionAdapter` (trigger/adapter/TimingDetectionAdapter.kt) bridges its output into
 * the trigger engine's [kaist.iclab.tracker.trigger.state.DetectionStateTracker], the same way
 * `StressDetectionAdapter` bridges `StressSensor`.
 */
class TimingSensor(
    private val context: Context,
    permissionManager: PermissionManager,
    private val configStorage: StateStorage<Config>,
    stateStorage: StateStorage<SensorState>,
    private val scheduleStorage: TimingScheduleStorage,
) : BaseSensor<TimingSensor.Config, TimingSensor.Entity>(
    permissionManager, configStorage, stateStorage, Config::class, Entity::class,
    titleResId = R.string.sensor_timing,
    descriptionResId = R.string.sensor_desc_timing,
    icon = Icons.Default.Schedule
) {
    companion object {
        private val TAG = TimingSensor::class.simpleName
        private val SCHEDULE_INTERVAL = TimeUnit.MINUTES.toMillis(15)

        /**
         * This sensor's `campaign_table.name` — i.e. `BaseSensor.id` ("Timing") run through the
         * app's `String.toCampaignSensorName()` convention (snake_case + "_sensor" suffix).
         * Kept here as a single source of truth since it's used both for the active-sensor
         * allowlist and to look up this sensor's `campaign_table.config` JSON.
         */
        const val CAMPAIGN_TABLE_NAME = "timing_sensor"
    }

    override val permissions: Array<String> = listOfNotNull(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Manifest.permission.SCHEDULE_EXACT_ALARM else null,
    ).toTypedArray()

    override val foregroundServiceTypes = arrayOf<Int>()

    private val timingActionName = "kaist.iclab.tracker.${name}_REQUEST"
    private val timingActionCode = 0x1235
    private val scheduleActionName = "kaist.iclab.tracker.timing_schedule_REQUEST"
    private val scheduleActionCode = 0x1236

    private val timingAlarmListener = SingleAlarmListener(
        context = context,
        actionName = timingActionName,
        actionCode = timingActionCode
    )

    private val scheduleCheckListener = AlarmListener(
        context = context,
        actionName = scheduleActionName,
        actionCode = scheduleActionCode,
        actionIntervalInMilliseconds = SCHEDULE_INTERVAL
    )

    /**
     * One named, independently-scheduled instant/window. `value` is what condition trees see
     * via `Detection(sensor="timing", value="<value>")` once it fires.
     */
    @Serializable
    data class TimingScheduleEntry(
        val value: String,
        val kind: String,
        val minInterval: Long? = null,
        val maxInterval: Long? = null,
        val startOfDay: Long? = null,
        val endOfDay: Long? = null,
        val numSurvey: Int? = null,
        val timeOfDay: List<Long>? = null,
    ) {
        fun toScheduleMethod(): SurveyScheduleMethod = when (kind.uppercase()) {
            "ESM" -> SurveyScheduleMethod.ESM(
                minInterval = minInterval ?: TimeUnit.HOURS.toMillis(1),
                maxInterval = maxInterval ?: TimeUnit.HOURS.toMillis(2),
                startOfDay = startOfDay ?: TimeUnit.HOURS.toMillis(8),
                endOfDay = endOfDay ?: TimeUnit.HOURS.toMillis(22),
                numSurvey = numSurvey ?: 3
            )

            "FIXED" -> SurveyScheduleMethod.Fixed(
                timeOfDay = timeOfDay ?: listOf(TimeUnit.HOURS.toMillis(12))
            )

            else -> SurveyScheduleMethod.Manual()
        }
    }

    data class Config(
        val schedules: List<TimingScheduleEntry> = emptyList()
    ) : SensorConfig {
        companion object {
            fun fromJson(jsonString: String): Config {
                val schedules = Json.decodeFromString<List<TimingScheduleEntry>>(jsonString)
                return Config(schedules)
            }
        }
    }

    @Serializable
    data class Entity(
        val value: String,
        val timestamp: Long,
    ) : SensorEntity()

    private fun getESMSchedule(baseDate: Long, config: SurveyScheduleMethod.ESM): List<Long> {
        val startTime = baseDate + config.startOfDay
        val endTime = baseDate + config.endOfDay
        val lengthOfDay = endTime - startTime

        val intervals = mutableListOf<Long>(0)
        repeat(config.numSurvey - 1) {
            val intervalLimit = lengthOfDay / (config.numSurvey - 1)
            val actualMaxInterval = config.maxInterval.coerceAtMost(intervalLimit)
            val actualMinInterval = config.minInterval.coerceAtMost(actualMaxInterval)

            // Try to spread out the schedule more (skewed to the maximum value)
            val skewedRandom = 1 - Math.random().pow(2.0)
            val interval = ((actualMaxInterval - actualMinInterval) * skewedRandom + actualMinInterval).toLong()
            intervals.add(interval)
        }

        val intervalSum = intervals.sum()
        val startMargin = (Math.random() * (lengthOfDay - intervalSum)).toLong()

        var accumulatedTime = startMargin + startTime
        val accumulatedTimeList = mutableListOf<Long>()

        intervals.forEach {
            accumulatedTime += it
            accumulatedTimeList.add(accumulatedTime)
        }

        return accumulatedTimeList
    }

    private fun getBaseDate(timestamp: Long, scheduleMethod: SurveyScheduleMethod): Long {
        val endOfDay = when (scheduleMethod) {
            is SurveyScheduleMethod.ESM -> scheduleMethod.endOfDay
            is SurveyScheduleMethod.Fixed -> scheduleMethod.timeOfDay.max()
            is SurveyScheduleMethod.Manual -> return 0
        }

        val zoneId = ZoneId.systemDefault()
        val dateTime = Instant.ofEpochMilli(timestamp).atZone(zoneId)

        val today = dateTime.toLocalDate().atStartOfDay(zoneId).toInstant().toEpochMilli()
        val todayEnd = today + endOfDay
        val endOfYesterday = todayEnd - TimeUnit.DAYS.toMillis(1)

        return if (timestamp < endOfYesterday) {
            today - TimeUnit.DAYS.toMillis(1)
        } else if (timestamp < todayEnd) {
            today
        } else {
            today + TimeUnit.DAYS.toMillis(1)
        }
    }

    private fun scheduleForDate(baseDate: Long, value: String) {
        Log.d(TAG, "Schedule $value using base date ${baseDate.formatLocalDateTime()}")
        val now = System.currentTimeMillis()
        val entry = configStorage.get().schedules.find { it.value == value } ?: return

        val scheduleMethod = entry.toScheduleMethod()
        val schedule = when (scheduleMethod) {
            is SurveyScheduleMethod.ESM -> getESMSchedule(baseDate, scheduleMethod)
            is SurveyScheduleMethod.Fixed -> scheduleMethod.timeOfDay.map { it + baseDate }
            else -> listOf()
        }.filter { it >= now }

        schedule.forEach {
            scheduleStorage.addSchedule(
                TimingSchedule(
                    value = value,
                    triggerTime = it
                )
            )
        }

        // Fail-safe for endless recursion
        if (baseDate - now >= TimeUnit.DAYS.toMillis(3)) return
        // If it is near the EOD, schedule for next day
        if (schedule.isEmpty() && scheduleMethod !is SurveyScheduleMethod.Manual) {
            scheduleForDate(baseDate + TimeUnit.DAYS.toMillis(1), value)
        }
    }

    private fun setupNextTimingSchedule() {
        val currentTime = System.currentTimeMillis()
        val entries = configStorage.get().schedules
        entries.filter { it.toScheduleMethod() !is SurveyScheduleMethod.Manual }.forEach { entry ->
            val nextSchedule = scheduleStorage.getNextSchedule(value = entry.value)

            if (nextSchedule == null) {
                val lastSchedule = scheduleStorage.getLastSchedule(value = entry.value)
                val scheduleMethod = entry.toScheduleMethod()
                val nextBaseDate = if (lastSchedule == null) {
                    getBaseDate(currentTime, scheduleMethod)
                } else {
                    getBaseDate(lastSchedule.triggerTime!!, scheduleMethod) + TimeUnit.DAYS.toMillis(1)
                }

                scheduleForDate(nextBaseDate, entry.value)
            }
        }

        val nextSchedule = scheduleStorage.getNextSchedule() ?: return
        val timeUntilNext = nextSchedule.triggerTime!! - currentTime
        if (timeUntilNext <= SCHEDULE_INTERVAL * 2) {
            Log.d(TAG, "Timing scheduled after $timeUntilNext ms! Using exact alarm for next wakeup")

            val bundle = Bundle()
            bundle.putString("scheduleId", nextSchedule.scheduleId)
            bundle.putString("value", nextSchedule.value)

            timingAlarmListener.scheduleNextAlarm(timeUntilNext, isExact = true, bundle = bundle)
        }
    }

    private val scheduleCheckCallback = { _: Intent? -> setupNextTimingSchedule() }

    private val timingCallback = timingCallback@{ intent: Intent? ->
        if (intent == null) return@timingCallback

        val scheduleId = intent.getStringExtra("scheduleId") ?: return@timingCallback
        val value = intent.getStringExtra("value") ?: return@timingCallback
        Log.d(TAG, "Timing fired (value: $value): $scheduleId")

        val now = System.currentTimeMillis()
        scheduleStorage.setFiredAt(scheduleId, now)
        listeners.forEach { it.invoke(Entity(value = value, timestamp = now)) }

        setupNextTimingSchedule()
    }

    override fun onStart() {
        scheduleCheckListener.addListener(scheduleCheckCallback)
        timingAlarmListener.addListener(timingCallback)

        scheduleCheckCallback(null)
    }

    override fun onStop() {
        scheduleCheckListener.removeListener(scheduleCheckCallback)
        timingAlarmListener.removeListener(timingCallback)

        scheduleStorage.resetSchedule()
    }
}
