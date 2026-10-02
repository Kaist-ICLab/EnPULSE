package kaist.iclab.wearabletracker.trigger

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kaist.iclab.tracker.sensor.microema.WatchSurveyConfig
import kaist.iclab.tracker.sync.ble.BLEDataChannel
import kaist.iclab.wearabletracker.Constants
import kaist.iclab.wearabletracker.ema.MicroEmaRepository
import kaist.iclab.wearabletracker.ema.WatchSurveyActivity
import kaist.iclab.wearabletracker.helpers.NotificationHelper
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Listens for the phone's command to launch a MicroEMA survey on the watch.
 *
 * Absorbs `WatchTriggerActionHandler.handleWatchEma`/`launchMicroEma`'s body verbatim — the only
 * thing that changed is the trigger source: it used to be a local [DefaultTriggerEngine]
 * dispatching a [kaist.iclab.tracker.trigger.model.TriggerActionConfig.WatchEma] action; now the
 * engine lives on the phone and sends the survey id directly over BLE
 * (`Constants.BLE.KEY_WATCH_EMA_TRIGGER`) once its own evaluation decides to fire it.
 */
class WatchEmaTriggerReceiver(
    private val context: Context,
    private val bleChannel: BLEDataChannel,
    private val microEmaRepository: MicroEmaRepository,
    private val surveyConfigReceiver: WatchSurveyConfigReceiver,
) {
    companion object {
        private const val TAG = "WatchEmaTriggerRcvr"
        private const val MAX_TRIGGER_AGE_MS = 30_000L
    }

    private var isListening = false

    fun startListening() {
        if (isListening) return
        isListening = true

        bleChannel.addOnReceivedListener(setOf(Constants.BLE.KEY_WATCH_EMA_TRIGGER)) { _, json ->
            handleWatchEmaTrigger(json)
        }

        Log.d(
            TAG,
            "Started listening for watch EMA triggers on BLE key: ${Constants.BLE.KEY_WATCH_EMA_TRIGGER}"
        )
    }

    private fun handleWatchEmaTrigger(json: JsonElement) {
        // Current phones send {"surveyId": N, "ts": epochMs}; older ones send the bare id.
        val surveyId: Int?
        val sentAt: Long?
        when (json) {
            is JsonObject -> {
                surveyId = json["surveyId"]?.jsonPrimitive?.intOrNull
                sentAt = json["ts"]?.jsonPrimitive?.longOrNull
            }
            is JsonPrimitive -> {
                surveyId = json.content.toIntOrNull()
                sentAt = null
            }
            else -> {
                surveyId = json.toString().toIntOrNull()
                sentAt = null
            }
        }

        if (surveyId == null) {
            Log.e(TAG, "Received WATCH_EMA_TRIGGER with invalid surveyId: $json")
            return
        }

        // Data Layer items queued while the watch was out of range all arrive on reconnect, by
        // then possibly on the next wearer's wrist.
        if (sentAt != null) {
            val ageMs = System.currentTimeMillis() - sentAt
            if (ageMs > MAX_TRIGGER_AGE_MS) {
                Log.w(TAG, "Dropping stale WATCH_EMA_TRIGGER for surveyId=$surveyId (${ageMs / 1000}s old)")
                return
            }
        }

        if (microEmaRepository.isSurveyActive) {
            Log.w(TAG, "Dropping WATCH_EMA_TRIGGER for surveyId=$surveyId: a survey is already open")
            return
        }

        val config = surveyConfigReceiver.getSurveyConfig(surveyId)
        if (config == null) {
            Log.w(TAG, "No survey config found for surveyId=$surveyId")
            return
        }

        Log.d(TAG, "Triggering MicroEMA: surveyId=$surveyId")

        // Update the active config in the repository
        microEmaRepository.updateConfig(config)

        // Launch the survey activity with vibration alert
        launchMicroEma(config)
    }

    /**
     * Vibrate and launch the MicroEMA survey activity using a High-Priority Notification
     * with a Full-Screen Intent. This bypasses background activity launch restrictions.
     */
    private fun launchMicroEma(config: WatchSurveyConfig) {
        try {
            // Double-buzz vibration to alert the user
            val vibrator =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    val vibratorManager = context.getSystemService(
                        Context.VIBRATOR_MANAGER_SERVICE
                    ) as VibratorManager
                    vibratorManager.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }
            vibrator.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200), -1)
            )

            val intent = Intent(context, WatchSurveyActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            NotificationHelper.showSurveyTriggerNotification(
                context = context,
                pendingIntent = pendingIntent,
                title = config.title,
                text = config.description ?: ""
            )

            // Also directly start the activity as a fallback — full-screen intents
            // on Wear OS may only show a heads-up notification without auto-launching.
            context.startActivity(intent)

            Log.d(TAG, "WatchSurveyActivity launched via Full-Screen Intent Notification")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch WatchSurveyActivity: ${e.message}", e)
        }
    }
}
