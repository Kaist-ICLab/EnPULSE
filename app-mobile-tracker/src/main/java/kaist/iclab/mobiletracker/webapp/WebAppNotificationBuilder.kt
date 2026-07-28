package kaist.iclab.mobiletracker.webapp

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import kaist.iclab.mobiletracker.Constants
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.utils.NotificationHelper

/**
 * Helper class to construct Intents and show notifications for WebApp triggers.
 * Decouples system UI and notification dispatching from WebAppTriggerHandler.
 */
object WebAppNotificationBuilder {
    private const val TAG = "WebAppNotificationBuilder"

    /**
     * Builds the Intent to open WebAppActivity with parameters.
     */
    fun createWebAppActivityIntent(
        context: Context,
        webAppId: String,
        surveyId: String,
        scheduleId: String,
        baseUrl: String
    ): Intent {
        val fullUrl = "$baseUrl?survey_id=$surveyId&schedule_id=$scheduleId"
        return Intent(context, WebAppActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            data = Uri.parse("webapp://$webAppId")
            putExtra(WebAppActivity.EXTRA_URL, fullUrl)
            putExtra(WebAppActivity.EXTRA_WEBAPP_ID, webAppId)
        }
    }

    /**
     * Shows a notification that triggers a specific WebApp.
     * 
     * [Use Case]
     * Used for internal EnPULSE survey campaigns (e.g., EMA questionnaires).
     * Opens WebAppActivity (WebView container) with survey_id and schedule_id query params.
     * This keeps the interaction completely inside the app and allows communication with native
     * features via the EnPulseBridge.
     */
    fun showWebAppTriggerNotification(
        context: Context,
        webAppId: String,
        surveyId: String,
        scheduleId: String,
        baseUrl: String
    ) {
        val intent = createWebAppActivityIntent(context, webAppId, surveyId, scheduleId, baseUrl)
        val pendingIntent = PendingIntent.getActivity(
            context,
            scheduleId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            NotificationHelper.showWebAppTriggerNotification(
                context = context,
                webAppId = webAppId,
                title = context.getString(R.string.web_apps_screen_title),
                text = webAppId,
                icon = R.drawable.ic_launcher_foreground,
                pendingIntent = pendingIntent,
                notificationId = Constants.Notification.ID_SURVEY_BASE + scheduleId.hashCode()
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to post webapp trigger notification due to SecurityException", e)
        }
    }

    /**
     * Shows a generic URL/notification trigger forwarded from Wearable.
     * 
     * [Use Case]
     * Used for general/external links (e.g., reading an article on WebMD or a university page).
     * Dispatches an Intent.ACTION_VIEW which opens the URL in the system default browser (e.g., Chrome),
     * rather than opening WebAppActivity inside the EnPULSE app.
     */
    fun showGenericTriggerNotification(
        context: Context,
        title: String,
        body: String,
        url: String
    ) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val uniqueString = "$title|$body|$url"
        val uniqueHash = uniqueString.hashCode()
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            uniqueHash,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            NotificationHelper.showGenericTriggerNotification(
                context = context,
                title = title,
                text = body,
                pendingIntent = pendingIntent,
                notificationId = Constants.Notification.ID_SURVEY_BASE + uniqueHash
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post notification", e)
        }
    }
}
