package kaist.iclab.wearabletracker.trigger

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import kaist.iclab.tracker.sync.ble.BLEDataChannel
import kaist.iclab.wearabletracker.Constants
import kaist.iclab.wearabletracker.MainActivity
import kaist.iclab.wearabletracker.helpers.NotificationHelper
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Listens for the phone's command to show a generic notification on the watch — a
 * [kaist.iclab.tracker.trigger.model.TriggerActionConfig.Notification] action authored with
 * `deviceType=1`. Mirrors [WatchEmaTriggerReceiver]'s shape: the trigger engine lives entirely on
 * the phone now, so this is purely a "show what you're told" receiver, no evaluation here.
 *
 * If the payload includes a `url`, tapping the notification fires [WatchNotificationTapReceiver],
 * which relays the url back to the phone to open (the watch has no browser of its own). Without a
 * `url`, tapping just opens the watch app directly.
 */
class WatchNotificationTriggerReceiver(
    private val context: Context,
    private val bleChannel: BLEDataChannel,
) {
    companion object {
        private const val TAG = "WatchNotificationRcvr"
    }

    private var isListening = false

    fun startListening() {
        if (isListening) return
        isListening = true

        bleChannel.addOnReceivedListener(setOf(Constants.BLE.KEY_WATCH_NOTIFICATION_TRIGGER)) { _, json ->
            handleNotificationTrigger(json)
        }

        Log.d(TAG, "Started listening for watch notification triggers on BLE key: ${Constants.BLE.KEY_WATCH_NOTIFICATION_TRIGGER}")
    }

    private fun handleNotificationTrigger(json: JsonElement) {
        try {
            val obj: JsonObject = when (json) {
                is JsonObject -> json
                is JsonPrimitive -> Json.parseToJsonElement(json.content).jsonObject
                else -> Json.parseToJsonElement(json.toString()).jsonObject
            }

            val title = obj["title"]?.jsonPrimitive?.content ?: ""
            val description = obj["description"]?.jsonPrimitive?.content ?: ""
            val url = obj["url"]?.jsonPrimitive?.content

            Log.d(TAG, "Triggering watch notification: title=$title, url=$url")

            val tapIntent = if (url != null) {
                Intent(context, WatchNotificationTapReceiver::class.java).apply {
                    putExtra(WatchNotificationTapReceiver.EXTRA_URL, url)
                }
            } else {
                Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
            }

            val pendingIntent = if (url != null) {
                PendingIntent.getBroadcast(
                    context,
                    0,
                    tapIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            } else {
                PendingIntent.getActivity(
                    context,
                    0,
                    tapIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }

            NotificationHelper.showGenericTriggerNotification(
                context = context,
                pendingIntent = pendingIntent,
                title = title,
                text = description
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error handling watch notification trigger: ${e.message}", e)
        }
    }
}
