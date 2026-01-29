package kaist.iclab.tracker.sensor.survey

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kaist.iclab.tracker.TrackerUtil.formatLapsedTime
import kaist.iclab.tracker.TrackerUtil.formatLocalDateTime
import kaist.iclab.tracker.listener.AlarmListener
import kaist.iclab.tracker.listener.BroadcastListener
import kaist.iclab.tracker.listener.SingleAlarmListener
import kaist.iclab.tracker.permission.PermissionManager
import kaist.iclab.tracker.sensor.core.BaseSensor
import kaist.iclab.tracker.sensor.core.SensorConfig
import kaist.iclab.tracker.sensor.core.SensorEntity
import kaist.iclab.tracker.sensor.core.SensorState
import kaist.iclab.tracker.sensor.survey.activity.DefaultSurveyActivity
import kaist.iclab.tracker.sensor.survey.activity.SurveyActivity
import kaist.iclab.tracker.storage.core.StateStorage
import kaist.iclab.tracker.storage.core.SurveyScheduleStorage
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import kotlin.math.pow

class SurveySensor(
    private val context: Context,
    permissionManager: PermissionManager,
    private val configStorage: StateStorage<Config>,
    private val stateStorage: StateStorage<SensorState>,
    private val scheduleStorage: SurveyScheduleStorage,
): BaseSensor<SurveySensor.Config, SurveySensor.Entity>(
    permissionManager, configStorage, stateStorage, Config::class, Entity::class
) {
    companion object {
        private val TAG = SurveySensor::class.simpleName
        private val SCHEDULE_INTERVAL = TimeUnit.MINUTES.toMillis(15)
        const val NOTIFICATION_CHANNEL_ID = "android_tracker_survey"
        const val NOTIFICATION_CHANNEL_NAME = "Survey"
        const val NOTIFICATION_ID = 1236478193

        const val RESULT_ACTION_NAME = "kaist.iclab.tracker.survey_sensor_result"
    }

    override val permissions: Array<String> = listOfNotNull(
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.POST_NOTIFICATIONS else null,
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Manifest.permission.SCHEDULE_EXACT_ALARM else null,
    ).toTypedArray()

    override val foregroundServiceTypes = arrayOf<Int>()

    private val surveyActionName = "kaist.iclab.tracker.${name}_REQUEST"
    private val surveyActionCode = 0x11
    private val scheduleActionName = "kaist.iclab.tracker.survey_schedule_REQUEST"
    private val scheduleActionCode = 0x1234

    private val surveyAlarmListener = SingleAlarmListener(
        context = context,
        actionName = surveyActionName,
        actionCode = surveyActionCode
    )

    private val scheduleCheckListener = AlarmListener(
        context = context,
        actionName = scheduleActionName,
        actionCode = scheduleActionCode,
        actionIntervalInMilliseconds = SCHEDULE_INTERVAL
    )

    private val surveyResultListener = BroadcastListener(
        context = context,
        actionNames = arrayOf(RESULT_ACTION_NAME)
    )

    data class Config (
        val survey: Map<String, Survey>
    ): SensorConfig

    @Serializable
    data class Entity (
        val triggerTime: Long? = null,
        val actualTriggerTime: Long? = null,
        val surveyStartTime: Long? = null,
        val responseSubmissionTime: Long? = null,
//        val title: String,
//        val message: String,
        val response: JsonElement,
    ): SensorEntity()

    override fun init() {
        super.init()
        // We are using separate channel for survey notification
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            "Notification to inform survey time"
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        SurveyActivity.initSurvey = { id: String, scheduleId: String? ->
            val requestedSurvey = configStorage.get().survey[id]!!
            if(scheduleId != null) scheduleStorage.setSurveyStartTime(scheduleId, System.currentTimeMillis())
            requestedSurvey.initSurveyResponse()
            requestedSurvey
        }
    }

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

        Log.v(TAG, "Intervals: ${intervals.map{ it.formatLapsedTime() }}")

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
        val endOfDay = when(scheduleMethod) {
            is SurveyScheduleMethod.ESM -> scheduleMethod.endOfDay
            is SurveyScheduleMethod.Fixed -> scheduleMethod.timeOfDay.max()
            is SurveyScheduleMethod.Manual -> return 0
        }

        val zoneId = ZoneId.systemDefault()
        val dateTime = java.time.Instant.ofEpochMilli(timestamp).atZone(zoneId)

        val todayStart = dateTime.toLocalDate().atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endOfYesterday = todayStart + endOfDay - TimeUnit.DAYS.toMillis(1)

        return if (timestamp < endOfYesterday) {
            // If current time is earlier than yesterday's window end,
            // the logical "base date" is yesterday.
            todayStart - TimeUnit.DAYS.toMillis(1)
        } else {
            // Otherwise, the logical base date is today.
            todayStart
        }
    }

    fun openSurvey(id: String) {
        val scheduleId = scheduleStorage.addSchedule(SurveySchedule(surveyId = id))
        val intent = Intent(context, DefaultSurveyActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("id", id)
            putExtra("scheduleId", scheduleId)
        }

        context.startActivity(intent)
    }

    private fun scheduleSurveyForDate(baseDate: Long): SurveySchedule? {
        Log.d(TAG, "BaseDate: ${baseDate.formatLocalDateTime()}")
        val now = System.currentTimeMillis()
        val config = configStorage.get()

        config.survey.forEach { id, survey ->
            val scheduleMethod = survey.scheduleMethod
            val schedule = when(scheduleMethod) {
                is SurveyScheduleMethod.ESM -> getESMSchedule(baseDate, scheduleMethod)
                is SurveyScheduleMethod.Fixed -> scheduleMethod.timeOfDay.map { it + baseDate }
                else -> listOf()
            }

            schedule.filter { it >= now }.forEach {
                scheduleStorage.addSchedule(SurveySchedule(
                    surveyId = id,
                    triggerTime = it
                ))
            }
        }

        return scheduleStorage.getNextSchedule()
    }

    private fun setupNextSurveySchedule() {
        val currentTime = System.currentTimeMillis()
        val surveys = configStorage.get().survey
        surveys.filter { it.value.scheduleMethod !is SurveyScheduleMethod.Manual }.forEach { id, survey ->
            val nextSchedule = scheduleStorage.getNextSchedule(surveyId = id)

            if(nextSchedule == null) {
                val lastSchedule = scheduleStorage.getLastSchedule()
                val nextBaseDate = if(lastSchedule == null) {
                    getBaseDate(currentTime, survey.scheduleMethod)
                } else {
                    getBaseDate(lastSchedule.triggerTime!!, survey.scheduleMethod) + TimeUnit.DAYS.toMillis(1)
                }
                scheduleSurveyForDate(nextBaseDate)
            }
        }

        val nextSchedule = scheduleStorage.getNextSchedule() ?: return
        val timeUntilNextSurvey = nextSchedule.triggerTime!! - currentTime
        if(timeUntilNextSurvey <= SCHEDULE_INTERVAL * 2) {
            Log.d(TAG, "Survey scheduled after $timeUntilNextSurvey ms! Using exact alarm for next wakeup")

            // Pass scheduleId data to the alarm
            val bundle = Bundle()
            bundle.putString("scheduleId", nextSchedule.scheduleId)
            bundle.putString("id", nextSchedule.surveyId)

            surveyAlarmListener.scheduleNextAlarm(timeUntilNextSurvey, isExact=true, bundle=bundle)
        }
    }

    private val scheduleCheckCallback = { intent: Intent? -> setupNextSurveySchedule() }

    private val surveyCallback = surveyCallback@{ intent: Intent? ->
        if(intent == null) return@surveyCallback

        val scheduleId = intent.getStringExtra("scheduleId")!!
        val surveyId = intent.getStringExtra("id")!!
        Log.d(TAG, "Survey triggered: $scheduleId")

        val notificationConfig = configStorage.get().survey[surveyId]!!.notificationConfig

        val surveyActivityIntent = Intent(context, DefaultSurveyActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("id", id)
            putExtra("scheduleId", scheduleId)
        }

        val pendingIntent = PendingIntent.getActivity(context, 0, surveyActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setOngoing(true)
            .setAutoCancel(true)
            .setContentTitle(notificationConfig.title)
            .setContentText(notificationConfig.description)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setSmallIcon(notificationConfig.icon)
            .setContentIntent(pendingIntent)
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }

        scheduleStorage.setActualTriggerTime(scheduleId, System.currentTimeMillis())
        setupNextSurveySchedule()
    }

    private val surveyResultCallback = surveyResultCallback@{ intent: Intent? ->
        Log.d(TAG, "surveyResultCallback invoked")

        if(intent == null) return@surveyResultCallback
        val scheduleId = intent.getStringExtra("scheduleId")!!
        val result = intent.getStringExtra("result")!!
        val responseTime = intent.getLongExtra("responseTime", -1)


        scheduleStorage.setResponseSubmissionTime(scheduleId, responseTime)
        val schedule = scheduleStorage.getScheduleByScheduleId(scheduleId)!!
        val resultJson = Json.decodeFromString<JsonElement>(result)

        listeners.forEach { it.invoke(
            Entity(
                response = resultJson,
                triggerTime = schedule.triggerTime,
                actualTriggerTime = schedule.actualTriggerTime,
                surveyStartTime = schedule.surveyStartTime,
                responseSubmissionTime = responseTime,
            )
        )}
    }

    override fun onStart() {
        scheduleCheckListener.addListener(scheduleCheckCallback)
        surveyAlarmListener.addListener(surveyCallback)
        surveyResultListener.addListener(surveyResultCallback)

        scheduleCheckCallback(null)
    }

    override fun onStop() {
        scheduleCheckListener.removeListener(scheduleCheckCallback)
        surveyAlarmListener.removeListener(surveyCallback)
        surveyResultListener.removeListener(surveyResultCallback)

        scheduleStorage.resetSchedule()
    }
}