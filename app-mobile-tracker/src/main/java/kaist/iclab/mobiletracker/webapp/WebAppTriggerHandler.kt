package kaist.iclab.mobiletracker.webapp

import android.content.Context
import android.content.Intent
import android.util.Log
import kaist.iclab.mobiletracker.Constants
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
            obj["extras"]?.jsonObject?.forEach { (key, extraVal) ->
                val extraObj = extraVal.jsonObject
                val value = extraObj["value"]?.jsonPrimitive?.content ?: ""
                val type = extraObj["type"]?.jsonPrimitive?.content?.lowercase() ?: "string"

                when (type) {
                    "int", "integer" -> intent.putExtra(key, value.toIntOrNull() ?: 0)
                    "long" -> intent.putExtra(key, value.toLongOrNull() ?: 0L)
                    "float" -> intent.putExtra(key, value.toFloatOrNull() ?: 0f)
                    "double" -> intent.putExtra(key, value.toDoubleOrNull() ?: 0.0)
                    "bool", "boolean" -> intent.putExtra(key, value.toBoolean())
                    else -> intent.putExtra(key, value) // Default to String
                }
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

        WebAppNotificationBuilder.showWebAppTriggerNotification(
            context = context,
            webAppId = webAppId,
            surveyId = surveyId,
            scheduleId = scheduleId,
            baseUrl = webApp.url
        )
    }

    fun launchNotification(title: String, body: String, url: String) {
        WebAppNotificationBuilder.showGenericTriggerNotification(
            context = context,
            title = title,
            body = body,
            url = url
        )
    }

    companion object {
        private const val TAG = "WebAppTriggerHandler"
    }
}
