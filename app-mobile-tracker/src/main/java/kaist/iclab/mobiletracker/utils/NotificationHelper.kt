package kaist.iclab.mobiletracker.utils

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import kaist.iclab.mobiletracker.Constants
import kaist.iclab.mobiletracker.MainActivity
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.utils.NotificationHelper.showSurveyTriggerNotification
import kaist.iclab.tracker.sensor.survey.SurveyNotificationConfig
import kaist.iclab.tracker.sensor.survey.activity.DefaultSurveyActivity

/**
 * Reusable utility for building and showing notifications.
 * Provides a simple API for creating notifications with common configurations.
 */
object NotificationHelper {

    /**
     * Checks if the app has permission to use full-screen intents (Android 14+).
     */
    fun canUseFullScreenIntent(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            return notificationManager.canUseFullScreenIntent()
        }
        return true
    }

    /**
     * Opens the system settings screen for full-screen intent permission.
     */
    fun openFullScreenIntentSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val intent =
                Intent(android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                    data = android.net.Uri.fromParts("package", context.packageName, null)
                }
            if (context !is android.app.Activity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    /**
     * Creates a PendingIntent that opens the MainActivity when clicked
     */
    fun createMainActivityPendingIntent(
        context: Context,
        requestCode: Int = 0
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    /**
     * Ensures a notification channel exists, creating it if necessary
     */
    fun ensureNotificationChannel(
        context: Context,
        channelId: String,
        channelName: String,
        importance: Int = NotificationManager.IMPORTANCE_DEFAULT
    ) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        if (notificationManager.getNotificationChannel(channelId) == null) {
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelName
            }
            notificationManager.createNotificationChannel(channel)
        }
//        }
    }

    /**
     * Builds a basic notification with common settings
     */
    fun buildNotification(
        context: Context,
        channelId: String,
        title: String,
        text: String,
        smallIcon: Int = R.drawable.ic_launcher_foreground,
        priority: Int = NotificationCompat.PRIORITY_DEFAULT,
        autoCancel: Boolean = true,
        ongoing: Boolean = false,
        pendingIntent: PendingIntent? = null
    ): NotificationCompat.Builder {
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(smallIcon)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(priority)
            .setAutoCancel(autoCancel)
            .setOngoing(ongoing)

        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent)
        }

        return builder
    }

    /**
     * Shows a notification
     */
    fun showNotification(
        context: Context,
        notificationId: Int,
        notification: android.app.Notification
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }

    /**
     * Show a notification for a scheduled survey.
     */
    fun showSurveyNotification(
        context: Context,
        surveyId: String,
        scheduleId: String,
        config: SurveyNotificationConfig,
        notificationId: Int
    ) {
        val channelId = "${Constants.Notification.CHANNEL_ID_SURVEY}_$surveyId"
        ensureNotificationChannel(
            context,
            channelId,
            Constants.Notification.CHANNEL_NAME_SURVEY,
            NotificationManager.IMPORTANCE_HIGH
        )

        val intent = Intent(context, DefaultSurveyActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("id", surveyId)
            putExtra("scheduleId", scheduleId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            scheduleId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = buildNotification(
            context = context,
            channelId = channelId,
            title = config.title,
            text = config.description,
            smallIcon = config.icon,
            priority = NotificationCompat.PRIORITY_HIGH,
            ongoing = true,
            pendingIntent = pendingIntent
        ).build()

        showNotification(context, notificationId, notification)
    }

    /**
     * Show a notification for a triggered survey (JIT).
     * Uses Full Screen Intent to wake the device.
     */
    @SuppressLint("FullScreenIntentPolicy")
    fun showSurveyTriggerNotification(
        context: Context,
        surveyId: String,
        scheduleId: String,
        config: SurveyNotificationConfig,
        notificationId: Int
    ) {
        val channelId = "${Constants.Notification.CHANNEL_ID_SURVEY_TRIGGER}_$surveyId"
        ensureNotificationChannel(
            context,
            channelId,
            Constants.Notification.CHANNEL_NAME_SURVEY_TRIGGER,
            NotificationManager.IMPORTANCE_HIGH
        )

        val intent = Intent(context, DefaultSurveyActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("id", surveyId)
            putExtra("scheduleId", scheduleId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            scheduleId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = buildNotification(
            context = context,
            channelId = channelId,
            title = config.title,
            text = config.description,
            smallIcon = config.icon,
            priority = NotificationCompat.PRIORITY_HIGH,
            ongoing = true,
            pendingIntent = pendingIntent
        ).apply {
            setCategory(NotificationCompat.CATEGORY_ALARM)
            setFullScreenIntent(pendingIntent, true)
        }.build()

        showNotification(context, notificationId, notification)
    }

    /**
     * Show a notification for a webapp trigger (JIT). Same shape as [showSurveyTriggerNotification]
     * but keyed by webAppId rather than surveyId, since a webapp trigger config is not itself a
     * [kaist.iclab.tracker.sensor.survey.SurveyNotificationConfig].
     */
    @SuppressLint("FullScreenIntentPolicy")
    fun showWebAppTriggerNotification(
        context: Context,
        webAppId: String,
        title: String,
        text: String,
        icon: Int,
        pendingIntent: PendingIntent,
        notificationId: Int
    ) {
        val channelId = "${Constants.Notification.CHANNEL_ID_SURVEY_TRIGGER}_webapp_$webAppId"
        ensureNotificationChannel(
            context,
            channelId,
            Constants.Notification.CHANNEL_NAME_SURVEY_TRIGGER,
            NotificationManager.IMPORTANCE_HIGH
        )

        val notification = buildNotification(
            context = context,
            channelId = channelId,
            title = title,
            text = text,
            smallIcon = icon,
            priority = NotificationCompat.PRIORITY_HIGH,
            ongoing = true,
            pendingIntent = pendingIntent
        ).apply {
            setCategory(NotificationCompat.CATEGORY_ALARM)
            setFullScreenIntent(pendingIntent, true)
        }.build()

        showNotification(context, notificationId, notification)
    }

    /**
     * Show a generic high-priority notification with a PendingIntent.
     */
    fun showGenericTriggerNotification(
        context: Context,
        title: String,
        text: String,
        pendingIntent: PendingIntent,
        notificationId: Int
    ) {
        val channelId = "${Constants.Notification.CHANNEL_ID_SURVEY_TRIGGER}_generic_${notificationId}"
        ensureNotificationChannel(
            context,
            channelId,
            Constants.Notification.CHANNEL_NAME_SURVEY_TRIGGER,
            NotificationManager.IMPORTANCE_HIGH
        )

        val notification = buildNotification(
            context = context,
            channelId = channelId,
            title = title,
            text = text,
            priority = NotificationCompat.PRIORITY_HIGH,
            ongoing = false,
            pendingIntent = pendingIntent
        ).apply {
            setCategory(NotificationCompat.CATEGORY_ALARM)
        }.build()

        showNotification(context, notificationId, notification)
    }
}
