package kaist.iclab.mobiletracker.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import kaist.iclab.mobiletracker.MainActivity
import kaist.iclab.mobiletracker.R

/**
 * Reusable utility for building and showing notifications.
 * Provides a simple API for creating notifications with common configurations.
 */
object NotificationHelper {

    /**
     * Creates a PendingIntent that opens the MainActivity when clicked
     */
    fun createMainActivityPendingIntent(
        context: Context,
        requestCode: Int = 0
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            if (notificationManager.getNotificationChannel(channelId) == null) {
                val channel = NotificationChannel(channelId, channelName, importance).apply {
                    description = channelName
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
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
}
