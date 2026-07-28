package kaist.iclab.mobiletracker.webapp

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import kaist.iclab.mobiletracker.Constants
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.utils.NotificationHelper
import kaist.iclab.tracker.sensor.survey.SurveySchedule
import kaist.iclab.tracker.storage.core.SurveyScheduleStorage

/**
 * Entry point shared by both trigger sources (BLE-forwarded from the watch, or a local phone
 * broadcast once the phone gets its own [kaist.iclab.tracker.trigger.TriggerEngine] instance —
 * see Step 0 of the platform plan). Issues a [SurveySchedule] the same way
 * [kaist.iclab.tracker.sensor.phone.SurveySensor.triggerSurveyNotification] does, then shows a
 * notification that opens [WebAppActivity].
 */
class WebAppTriggerHandler(
    private val context: Context,
    private val scheduleStorage: SurveyScheduleStorage,
    private val webAppRegistry: WebAppRegistry
) {
    fun launch(surveyId: String, webAppId: String) {
        val webApp = webAppRegistry.get(webAppId)
        if (webApp == null) {
            Log.e(TAG, "Unknown webAppId: $webAppId")
            return
        }

        // Same pattern as SurveySensor.triggerSurveyNotification: triggerTime is recorded at
        // addSchedule time and never overwritten later.
        val scheduleId = scheduleStorage.addSchedule(
            SurveySchedule(surveyId = surveyId, triggerTime = System.currentTimeMillis())
        )

        val fullUrl = "${webApp.url}?survey_id=$surveyId&schedule_id=$scheduleId"

        val intent = Intent(context, WebAppActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            data = android.net.Uri.parse("webapp://$webAppId")
            putExtra(WebAppActivity.EXTRA_URL, fullUrl)
            putExtra(WebAppActivity.EXTRA_WEBAPP_ID, webAppId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            scheduleId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            // WebAppConfig no longer carries notification copy/icon, so this falls back to
            // generic text naming the webapp. Revisit once WebAppConfig grows that metadata back
            // (or add it to the Phase 2 campaign_webapp table).
            NotificationHelper.showWebAppTriggerNotification(
                context = context,
                webAppId = webAppId,
                title = context.getString(R.string.web_apps_screen_title),
                text = webApp.id,
                icon = R.drawable.ic_launcher_foreground,
                pendingIntent = pendingIntent,
                notificationId = Constants.Notification.ID_SURVEY_BASE + scheduleId.hashCode()
            )
        } catch (e: SecurityException) {
            // Matches SurveySensor.triggerSurveyNotification: the schedule is intentionally not
            // rolled back here, for consistency with the existing survey trigger path.
            Log.e(TAG, "Failed to post webapp trigger notification due to SecurityException", e)
        }
    }

    fun launchNotification(title: String, body: String, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            url.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            NotificationHelper.showGenericTriggerNotification(
                context = context,
                title = title,
                text = body,
                pendingIntent = pendingIntent,
                notificationId = Constants.Notification.ID_SURVEY_BASE + url.hashCode()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post notification", e)
        }
    }

    companion object {
        private const val TAG = "WebAppTriggerHandler"
    }
}
