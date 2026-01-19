package kaist.iclab.wearabletracker.helpers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import kaist.iclab.wearabletracker.Constants
import kaist.iclab.wearabletracker.R

/**
 * Helper class for creating and showing notifications in the wearable tracker app.
 * Provides reusable methods for common notification patterns.
 */
object NotificationHelper {
    /**
     * Notification channel configurations
     */
    enum class NotificationChannelConfig(
        val channelId: String,
        val channelName: String,
        val description: String
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
            NotificationManager.IMPORTANCE_DEFAULT
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
}

