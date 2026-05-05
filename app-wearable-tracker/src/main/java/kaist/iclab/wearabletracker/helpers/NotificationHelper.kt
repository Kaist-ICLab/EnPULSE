package kaist.iclab.wearabletracker.helpers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import kaist.iclab.tracker.sensor.controller.BackgroundController
import kaist.iclab.wearabletracker.Constants
import kaist.iclab.wearabletracker.R

/**
 * Helper class for creating and showing notifications in the wearable tracker app.
 * Provides reusable methods for common notification patterns.
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
     * Notification channel configurations
     */
    enum class NotificationChannelConfig(
        val channelId: String,
        val channelName: String,
        val description: String,
        val importance: Int = NotificationManager.IMPORTANCE_DEFAULT
    ) {
        UPLOAD_DATA(
            Constants.NotificationChannel.UPLOAD_DATA_ID,
            Constants.NotificationChannel.UPLOAD_DATA_NAME,
            Constants.NotificationChannel.UPLOAD_DATA_DESCRIPTION
        ),
        FLUSH_DATA(
            Constants.NotificationChannel.FLUSH_DATA_ID,
            Constants.NotificationChannel.FLUSH_DATA_NAME,
            Constants.NotificationChannel.FLUSH_DATA_DESCRIPTION
        ),
        ERROR(
            Constants.NotificationChannel.ERROR_ID,
            Constants.NotificationChannel.ERROR_NAME,
            Constants.NotificationChannel.ERROR_DESCRIPTION
        ),
        TRIGGER(
            Constants.NotificationChannel.TRIGGER_ID,
            Constants.NotificationChannel.TRIGGER_NAME,
            Constants.NotificationChannel.TRIGGER_DESCRIPTION,
            NotificationManager.IMPORTANCE_HIGH
        )
    }

    /**
     * Ensure a notification channel exists. Safe to call multiple times.
     */
    fun ensureNotificationChannel(
        context: Context,
        config: NotificationChannelConfig
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            config.channelId,
            config.channelName,
            config.importance
        ).apply {
            description = config.description
        }
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Show a success notification
     */
    fun showSuccessNotification(
        context: Context,
        channelConfig: NotificationChannelConfig,
        title: String,
        message: String,
        notificationId: Int
    ) {
        ensureNotificationChannel(context, channelConfig)
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, channelConfig.channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    /**
     * Show a failure notification
     */
    fun showFailureNotification(
        context: Context,
        channelConfig: NotificationChannelConfig,
        title: String,
        message: String,
        notificationId: Int
    ) {
        ensureNotificationChannel(context, channelConfig)
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, channelConfig.channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    /**
     * Helper method to format exception message and show failure notification
     */
    private fun showFailureWithException(
        context: Context,
        exception: Throwable,
        contextInfo: String?,
        channelConfig: NotificationChannelConfig,
        title: String,
        notificationId: Int
    ) {
        val message = buildString {
            contextInfo?.let { append("$it: ") }
            append(exception.message ?: "Unknown error")
            // Truncate message if too long (notification text has limits)
            if (length > 200) {
                setLength(200)
                append("...")
            }
        }
        showFailureNotification(
            context = context,
            channelConfig = channelConfig,
            title = title,
            message = message,
            notificationId = notificationId
        )
    }

    /**
     * Show phone communication success notification
     */
    fun showPhoneCommunicationSuccess(context: Context) {
        showSuccessNotification(
            context = context,
            channelConfig = NotificationChannelConfig.UPLOAD_DATA,
            title = context.getString(R.string.notification_upload_success_title),
            message = context.getString(R.string.notification_upload_success_message),
            notificationId = Constants.NotificationId.UPLOAD_DATA_SUCCESS
        )
    }

    /**
     * Show phone communication failure notification
     */
    fun showPhoneCommunicationFailure(
        context: Context,
        exception: Throwable,
        contextInfo: String? = null
    ) {
        showFailureWithException(
            context = context,
            exception = exception,
            contextInfo = contextInfo,
            channelConfig = NotificationChannelConfig.UPLOAD_DATA,
            title = context.getString(R.string.notification_upload_failure_title),
            notificationId = Constants.NotificationId.UPLOAD_DATA_FAILURE
        )
    }

    /**
     * Show phone communication failure notification with message string (for non-exception cases)
     * Internally creates an exception to reuse the exception handling logic
     */
    fun showPhoneCommunicationFailure(context: Context, message: String) {
        showPhoneCommunicationFailure(
            context = context,
            exception = RuntimeException(message),
            contextInfo = null
        )
    }

    /**
     * Show flush operation success notification
     */
    fun showFlushSuccess(context: Context) {
        showSuccessNotification(
            context = context,
            channelConfig = NotificationChannelConfig.FLUSH_DATA,
            title = context.getString(R.string.notification_flush_success_title),
            message = context.getString(R.string.notification_flush_success_message),
            notificationId = Constants.NotificationId.FLUSH_DATA_SUCCESS
        )
    }

    /**
     * Show flush operation failure notification
     */
    fun showFlushFailure(context: Context, exception: Throwable, contextInfo: String? = null) {
        showFailureWithException(
            context = context,
            exception = exception,
            contextInfo = contextInfo,
            channelConfig = NotificationChannelConfig.FLUSH_DATA,
            title = context.getString(R.string.notification_flush_failure_title),
            notificationId = Constants.NotificationId.FLUSH_DATA_FAILURE
        )
    }

    /**
     * Show exception notification with formatted error message
     */
    fun showException(context: Context, exception: Throwable, contextInfo: String? = null) {
        val title = "Error: ${exception.javaClass.simpleName}"
        val message = buildString {
            contextInfo?.let { append("$it: ") }
            append(exception.message ?: "Unknown error")
            // Truncate message if too long (notification text has limits)
            if (length > 200) {
                setLength(200)
                append("...")
            }
        }
        showError(context, title, message)
    }

    /**
     * Show error/exception notification
     */
    fun showError(context: Context, title: String, message: String) {
        // Use a unique notification ID based on current time to allow multiple error notifications
        val notificationId =
            Constants.NotificationId.ERROR + (System.currentTimeMillis() % 1000).toInt()
        showFailureNotification(
            context = context,
            channelConfig = NotificationChannelConfig.ERROR,
            title = title,
            message = message,
            notificationId = notificationId
        )
    }

    /**
     * Show a notification for a survey trigger (MicroEMA).
     * Uses High Priority and Full Screen Intent to wake the watch.
     */
    fun showSurveyTriggerNotification(
        context: Context,
        pendingIntent: PendingIntent,
        title: String,
        text: String
    ) {
        ensureNotificationChannel(context, NotificationChannelConfig.TRIGGER)
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification =
            NotificationCompat.Builder(context, NotificationChannelConfig.TRIGGER.channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setFullScreenIntent(pendingIntent, true)
                .setAutoCancel(true)
                .build()

        notificationManager.notify(Constants.NotificationId.TRIGGER, notification)
    }

    /**
     * Build a notification for a background service.
     */
    fun buildServiceNotification(
        context: Context,
        config: BackgroundController.ServiceNotification,
        pendingIntent: PendingIntent? = null
    ): Notification {
        ensureNotificationChannel(
            context,
            config.channelId,
            config.channelName,
            NotificationManager.IMPORTANCE_LOW // Low priority for ongoing services
        )

        val builder = NotificationCompat.Builder(context, config.channelId)
            .setSmallIcon(config.icon)
            .setContentTitle(config.title)
            .setContentText(config.description)
            .setOngoing(true)

        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent)
        }

        return builder.build()
    }

    /**
     * Ensure a notification channel exists using raw strings (for library integration).
     */
    fun ensureNotificationChannel(
        context: Context,
        channelId: String,
        channelName: String,
        importance: Int
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (notificationManager.getNotificationChannel(channelId) == null) {
            val channel = NotificationChannel(channelId, channelName, importance)
            notificationManager.createNotificationChannel(channel)
        }
    }
}

