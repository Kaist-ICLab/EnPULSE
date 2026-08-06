package kaist.iclab.tracker.sensor.phone

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Poll
import kaist.iclab.tracker.R
import kaist.iclab.tracker.listener.BroadcastListener
import kaist.iclab.tracker.permission.PermissionManager
import kaist.iclab.tracker.sensor.core.BaseSensor
import kaist.iclab.tracker.sensor.core.SensorConfig
import kaist.iclab.tracker.sensor.core.SensorEntity
import kaist.iclab.tracker.sensor.core.SensorState
import kaist.iclab.tracker.sensor.survey.Survey
import kaist.iclab.tracker.sensor.survey.SurveyNotificationConfig
import kaist.iclab.tracker.sensor.survey.SurveySchedule
import kaist.iclab.tracker.sensor.survey.activity.DefaultSurveyActivity
import kaist.iclab.tracker.sensor.survey.activity.SurveyActivity
import kaist.iclab.tracker.sensor.survey.config.SurveyBuilder
import kaist.iclab.tracker.sensor.survey.config.SurveyConfig
import kaist.iclab.tracker.storage.core.StateStorage
import kaist.iclab.tracker.storage.core.SurveyScheduleStorage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * Hosts survey UI and collects responses. As of the trigger-system unification, this sensor no
 * longer decides *when* a survey fires — that's the trigger engine's job now (a `Detection`
 * condition, possibly gated by [kaist.iclab.tracker.sensor.phone.TimingSensor] for time-based
 * cases, combined with an `Ema`/`WatchEma` action). [triggerSurveyNotification] is invoked by the
 * app's `PhoneTriggerActionHandler` whenever an `Ema` action fires; [openSurvey] remains available
 * for manual/immediate opening (e.g. a "preview this survey" debug action in settings).
 */
class SurveySensor(
    private val context: Context,
    permissionManager: PermissionManager,
    private val configStorage: StateStorage<Config>,
    stateStorage: StateStorage<SensorState>,
    private val scheduleStorage: SurveyScheduleStorage,
): BaseSensor<SurveySensor.Config, SurveySensor.Entity>(
    permissionManager, configStorage, stateStorage, Config::class, Entity::class,
    titleResId = R.string.sensor_survey,
    descriptionResId = R.string.sensor_desc_survey,
    icon = Icons.Default.Poll
) {
    companion object {
        private val TAG = SurveySensor::class.simpleName
        const val NOTIFICATION_CHANNEL_ID = "android_tracker_survey"
        const val NOTIFICATION_CHANNEL_NAME = "Survey"
        const val NOTIFICATION_ID = 1236478193

        const val RESULT_ACTION_NAME = "kaist.iclab.tracker.survey_sensor_result"
    }

    override val permissions: Array<String> = listOfNotNull(
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.POST_NOTIFICATIONS else null,
    ).toTypedArray()

    override val foregroundServiceTypes = arrayOf<Int>()

    private val surveyResultListener = BroadcastListener(
        context = context,
        actionNames = arrayOf(RESULT_ACTION_NAME)
    )

    data class Config (
        val survey: Map<String, Survey>
    ): SensorConfig {
        companion object {
            fun fromJson(jsonString: String): Config {
                val surveyConfigs = Json.decodeFromString<Map<String, SurveyConfig>>(jsonString)
                val surveys = surveyConfigs.mapValues { (_, config) ->
                    SurveyBuilder.build(config)
                }
                return Config(surveys)
            }
        }
    }

    @Serializable
    data class Entity (
        val triggerTime: Long? = null,
        val actualTriggerTime: Long? = null,
        val surveyStartTime: Long? = null,
        val responseSubmissionTime: Long? = null,
        val response: JsonElement,
        @SerialName("device_type") val deviceType: Int? = null, // 0 = phone, 1 = watch
    ): SensorEntity()

    override fun init() {
        super.init()
        // We are using separate channel for each survey notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        configStorage.get().survey.forEach {
            val channelId = NOTIFICATION_CHANNEL_ID + "_" + it.key
            val channel = NotificationChannel(
                channelId,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                "Notification to inform survey time (surveyId: ${it.key}"
            }

            notificationManager.createNotificationChannel(channel)
        }


        SurveyActivity.initSurvey = { id: String, scheduleId: String? ->
            val requestedSurvey = configStorage.get().survey[id]!!
            if(scheduleId != null) scheduleStorage.setSurveyStartTime(scheduleId, System.currentTimeMillis())
            requestedSurvey.initSurveyResponse()
            requestedSurvey
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

    /**
     * Trigger a survey by posting a High-Priority Notification.
     * This is safe to call from the background (e.g., when the trigger engine's action handler
     * dispatches an `Ema` action) because it uses a Full-Screen Intent / High Priority
     * Notification rather than launching the Activity directly.
     */
    fun triggerSurveyNotification(id: String) {
        val config = configStorage.get().survey[id]
        if (config == null) {
            Log.e(TAG, "Cannot trigger survey notification: Survey ID $id not found in config")
            return
        }

        val scheduleId = scheduleStorage.addSchedule(SurveySchedule(surveyId = id, triggerTime = System.currentTimeMillis()))
        val notificationConfig = config.notificationConfig

        val handler = notificationHandler
        if (handler != null) {
            handler.showTriggeredNotification(id, scheduleId, notificationConfig)
            return
        }

        // Fallback to internal notification logic if no handler is provided
        val surveyActivityIntent = Intent(context, DefaultSurveyActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("id", id)
            putExtra("scheduleId", scheduleId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            scheduleId.hashCode(), // Unique request code
            surveyActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID + "_" + id)
            .setOngoing(true)
            .setAutoCancel(true)
            .setContentTitle(notificationConfig.title)
            .setContentText(notificationConfig.description)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM) // Use ALARM for triggers
            .setSmallIcon(notificationConfig.icon)
            .setFullScreenIntent(pendingIntent, true) // Add full screen intent to wake device

        try {
            // Use a unique notification ID based on scheduleId to ensure every trigger alerts the user
            // even if a previous one is still active.
            val triggerNotificationId = NOTIFICATION_ID + scheduleId.hashCode()
            NotificationManagerCompat.from(context).notify(triggerNotificationId, builder.build())
            Log.d(TAG, "Triggered phone survey notification (survey: $id, schedule: $scheduleId)")
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to post survey notification due to SecurityException", e)
        }
    }

    interface NotificationHandler {
        fun showTriggeredNotification(surveyId: String, scheduleId: String, config: SurveyNotificationConfig)
    }

    var notificationHandler: NotificationHandler? = null

    private val surveyResultCallback = surveyResultCallback@{ intent: Intent? ->
        if(intent == null) return@surveyResultCallback
        val scheduleId = intent.getStringExtra("scheduleId") ?: return@surveyResultCallback
        val result = intent.getStringExtra("result") ?: return@surveyResultCallback
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
                deviceType = 0 // Phone
            )
        )}
    }

    override fun onStart() {
        surveyResultListener.addListener(surveyResultCallback)
    }

    override fun onStop() {
        surveyResultListener.removeListener(surveyResultCallback)
        scheduleStorage.resetSchedule()
    }
}
