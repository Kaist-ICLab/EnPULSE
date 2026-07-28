package kaist.iclab.mobiletracker.webapp

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import kaist.iclab.mobiletracker.Constants
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.utils.NotificationHelper
import kaist.iclab.tracker.sensor.survey.SurveySchedule
import kaist.iclab.tracker.storage.core.SurveyScheduleStorage
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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
    /**
     * Helper to check if a forwarded BLE trigger has expired (stale).
     * Prevents execution of outdated triggers queued while devices were disconnected.
     */
    private fun isStale(json: JsonElement, kind: String): Boolean {
        try {
            val timestamp = json.jsonObject["timestamp"]?.jsonPrimitive?.content?.toLongOrNull()
            if (timestamp != null) {
                val elapsed = System.currentTimeMillis() - timestamp
                if (elapsed > Constants.Trigger.STALE_THRESHOLD_MS) {
                    Log.w(TAG, "Discarded stale BLE $kind trigger: elapsed ${elapsed / 1000}s > ${Constants.Trigger.STALE_THRESHOLD_MS / 1000}s limit")
                    return true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking trigger staleness", e)
        }
        return false
    }

    /**
     * Parses and processes incoming WEBAPP_TRIGGER payload forwarded from the watch.
     */
    fun handleWebAppTriggerPayload(json: JsonElement) {
        if (isStale(json, "WEBAPP")) return
        try {
            val obj = json.jsonObject
            val surveyId = obj["survey_id"]?.jsonPrimitive?.content
            val webAppId = obj["webapp_id"]?.jsonPrimitive?.content
            if (surveyId == null || webAppId == null) {
                Log.e(TAG, "Received WEBAPP_TRIGGER with missing survey_id/webapp_id: $json")
                return
            }
            Log.d(TAG, "Handling WEBAPP_TRIGGER for surveyId=$surveyId webAppId=$webAppId")
            launch(surveyId, webAppId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse WEBAPP_TRIGGER payload: ${e.message}", e)
        }
    }

    /**
     * Parses and processes incoming NOTIFICATION_TRIGGER payload forwarded from the watch.
     */
    fun handleNotificationTriggerPayload(json: JsonElement) {
        if (isStale(json, "NOTIFICATION")) return
        try {
            val obj = json.jsonObject
            val title = obj["title"]?.jsonPrimitive?.content ?: ""
            val body = obj["body"]?.jsonPrimitive?.content ?: ""
            val url = obj["url"]?.jsonPrimitive?.content
            if (url == null) {
                Log.e(TAG, "Received NOTIFICATION_TRIGGER with missing url: $json")
                return
            }
            Log.d(TAG, "Handling NOTIFICATION_TRIGGER for url=$url")
            launchNotification(title, body, url)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse NOTIFICATION_TRIGGER payload: ${e.message}", e)
        }
    }

    /**
     * Parses and processes incoming BROADCAST_TRIGGER payload forwarded from the watch.
     */
    fun handleBroadcastTriggerPayload(json: JsonElement) {
        if (isStale(json, "BROADCAST")) return
        try {
            val obj = json.jsonObject
            val action = obj["action"]?.jsonPrimitive?.content
            if (action == null) {
                Log.e(TAG, "Received BROADCAST_TRIGGER with missing action: $json")
                return
            }
            Log.d(TAG, "Handling BROADCAST_TRIGGER action=$action")
            val intent = Intent(action)
            obj["extras"]?.jsonObject?.forEach { (key, value) ->
                intent.putExtra(key, value.jsonPrimitive.content)
            }
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send broadcast/parse payload for BROADCAST_TRIGGER: ${e.message}", e)
        }
    }

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
            data = Uri.parse("webapp://$webAppId")
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

    companion object {
        private const val TAG = "WebAppTriggerHandler"
    }
}
