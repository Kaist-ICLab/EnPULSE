package kaist.iclab.mobiletracker.trigger

import android.content.Context
import android.content.Intent
import android.util.Log
import kaist.iclab.mobiletracker.Constants
import kaist.iclab.mobiletracker.config.AppConfig
import kaist.iclab.mobiletracker.webapp.WebAppTriggerHandler
import kaist.iclab.tracker.sensor.phone.SurveySensor
import kaist.iclab.tracker.sync.ble.BLEDataChannel
import kaist.iclab.tracker.trigger.engine.TriggerActionHandler
import kaist.iclab.tracker.trigger.model.ParsedCampaignTrigger
import kaist.iclab.tracker.trigger.model.TriggerActionConfig
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Phone-side [TriggerActionHandler] — the trigger engine now lives entirely on the phone (see
 * [kaist.iclab.mobiletracker.di.phone.triggerModule]), so all four action kinds execute directly
 * here except [TriggerActionConfig.WatchEma] and a watch-targeted
 * [TriggerActionConfig.Notification] (`deviceType=1`), which need one BLE hop to tell the watch to
 * launch its MicroEMA UI / show the notification (mirrors
 * [kaist.iclab.wearabletracker] `WatchEmaTriggerReceiver`/`WatchNotificationTriggerReceiver` on
 * the other end). Native Wear OS notification bridging was tried for the watch notification case
 * and didn't reliably show up, so this stays on the explicit BLE path.
 */
class PhoneTriggerActionHandler(
    private val context: Context,
    private val surveySensor: SurveySensor,
    private val webAppTriggerHandler: WebAppTriggerHandler,
    private val bleChannel: BLEDataChannel,
) : TriggerActionHandler {

    companion object {
        private const val TAG = "PhoneTriggerAction"
    }

    override suspend fun onAction(trigger: ParsedCampaignTrigger, action: TriggerActionConfig) {
        when (action) {
            is TriggerActionConfig.Ema -> handleEma(action)
            is TriggerActionConfig.WatchEma -> handleWatchEma(trigger, action)
            is TriggerActionConfig.Broadcast -> handleBroadcast(action)
            is TriggerActionConfig.Notification -> handleNotification(trigger, action)
        }
    }

    private fun handleEma(action: TriggerActionConfig.Ema) {
        Log.d(TAG, "Triggering phone EMA: surveyId=${action.surveyId}")
        surveySensor.triggerSurveyNotification(action.surveyId.toString())
    }

    private suspend fun handleWatchEma(trigger: ParsedCampaignTrigger, action: TriggerActionConfig.WatchEma) {
        Log.d(TAG, "Sending WatchEma trigger to watch: surveyId=${action.surveyId}, trigger=${trigger.name}")
        try {
            bleChannel.send(AppConfig.BLEKeys.WATCH_EMA_TRIGGER, action.surveyId.toString(), isUrgent = true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send WatchEma trigger to watch: ${e.message}", e)
        }
    }

    private fun handleBroadcast(action: TriggerActionConfig.Broadcast) {
        // The Dashboard's "open webapp" trigger preset is authored as a Broadcast action with
        // this well-known action id, but it isn't a real Android broadcast contract — no receiver
        // is registered for it (mirrors the old WatchTriggerActionHandler.forwardWebAppTriggerToPhone
        // special case, minus the BLE hop since we're already on the phone now).
        if (action.action == Constants.Trigger.ACTION_OPEN_WEBAPP) {
            handleOpenWebApp(action)
            return
        }

        Log.d(TAG, "Sending broadcast: action=${action.action}")
        try {
            val intent = Intent(action.action)
            action.extras.forEach { extra ->
                when (extra.valueType.lowercase()) {
                    "int", "integer" -> intent.putExtra(extra.key, extra.value.toIntOrNull() ?: 0)
                    "long" -> intent.putExtra(extra.key, extra.value.toLongOrNull() ?: 0L)
                    "float" -> intent.putExtra(extra.key, extra.value.toFloatOrNull() ?: 0f)
                    "double" -> intent.putExtra(extra.key, extra.value.toDoubleOrNull() ?: 0.0)
                    "bool", "boolean" -> intent.putExtra(extra.key, extra.value.toBoolean())
                    else -> intent.putExtra(extra.key, extra.value)
                }
            }
            intent.setPackage(context.packageName)
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send broadcast: ${e.message}", e)
        }
    }

    private fun handleOpenWebApp(action: TriggerActionConfig.Broadcast) {
        val surveyId = action.extras.firstOrNull { it.key == "survey_id" }?.value
        val webAppId = action.extras.firstOrNull { it.key == "webapp_id" }?.value
        if (surveyId == null || webAppId == null) {
            Log.e(TAG, "OPEN_WEBAPP broadcast missing survey_id/webapp_id extras")
            return
        }

        Log.d(TAG, "Launching webapp: surveyId=$surveyId webAppId=$webAppId")
        webAppTriggerHandler.launch(surveyId, webAppId)
    }

    private suspend fun handleNotification(trigger: ParsedCampaignTrigger, action: TriggerActionConfig.Notification) {
        if (action.deviceType == 1) {
            handleWatchNotification(trigger, action)
            return
        }

        Log.d(TAG, "Showing notification: title=${action.title}, url=${action.url}")
        webAppTriggerHandler.launchNotification(action.title, action.description, action.url)
    }

    /**
     * deviceType=1 — show the notification on the watch instead, over BLE (native Wear OS
     * bridging didn't reliably show up in testing). [TriggerActionConfig.Notification.url] is
     * still sent — if the watch notification is tapped and a url was set, the watch relays it
     * back to the phone (see `WatchNotificationTapReceiver` on the watch side and
     * [AppConfig.BLEKeys.PHONE_OPEN_URL_TRIGGER]/[kaist.iclab.mobiletracker.helpers.BLEHelper])
     * so the phone opens it there, since the watch has no browser of its own.
     */
    private suspend fun handleWatchNotification(trigger: ParsedCampaignTrigger, action: TriggerActionConfig.Notification) {
        Log.d(TAG, "Sending watch notification: title=${action.title}, url=${action.url}, trigger=${trigger.name}")
        try {
            val payload = buildJsonObject {
                put("title", action.title)
                put("description", action.description)
                if (action.url != null) put("url", action.url)
            }.toString()
            bleChannel.send(AppConfig.BLEKeys.WATCH_NOTIFICATION_TRIGGER, payload, isUrgent = true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send watch notification: ${e.message}", e)
        }
    }
}
