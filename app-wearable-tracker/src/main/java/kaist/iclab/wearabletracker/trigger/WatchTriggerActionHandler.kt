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
import kaist.iclab.tracker.trigger.engine.TriggerActionHandler
import kaist.iclab.tracker.trigger.model.ParsedCampaignTrigger
import kaist.iclab.tracker.trigger.model.TriggerActionConfig
import kaist.iclab.wearabletracker.Constants
import kaist.iclab.wearabletracker.ema.MicroEmaRepository
import kaist.iclab.wearabletracker.ema.WatchSurveyActivity
import kaist.iclab.wearabletracker.helpers.NotificationHelper
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Watch-specific implementation of [TriggerActionHandler].
 *
 * Handles actions triggered by the [DefaultTriggerEngine] when condition trees
 * evaluate to true. Currently supports:
 * - [TriggerActionConfig.WatchEma] — launches a MicroEMA survey on the watch
 *
 * Future:
 * - [TriggerActionConfig.Ema] — send to phone via BLE for phone-side survey
 * - [TriggerActionConfig.Broadcast] — send a local Android broadcast
 */
class WatchTriggerActionHandler(
    private val context: Context,
    private val microEmaRepository: MicroEmaRepository,
    private val bleChannel: BLEDataChannel
) : TriggerActionHandler {

    companion object {
        private const val TAG = "WatchTriggerAction"
    }

    /**
     * Survey configurations keyed by survey ID.
     * Updated when trigger config is received from phone via BLE.
     */
    private var surveyConfigs: Map<Int, WatchSurveyConfig> = emptyMap()

    /**
     * Update the available survey configurations.
     * Called by [TriggerConfigReceiver] when new config arrives from the phone.
     */
    fun updateSurveyConfigs(configs: Map<Int, WatchSurveyConfig>) {
        this.surveyConfigs = configs

        // Reset the queue progress for these surveys so they start from Q1
        configs.keys.forEach { surveyId ->
            microEmaRepository.resetQueueProgress(surveyId)
        }

        Log.d(TAG, "Updated survey configs and reset queue progress: ${configs.keys}")
    }

    override suspend fun onAction(
        trigger: ParsedCampaignTrigger,
        action: TriggerActionConfig
    ) {
        when (action) {
            is TriggerActionConfig.WatchEma -> handleWatchEma(trigger, action)
            is TriggerActionConfig.Ema -> handleEma(trigger, action)

            is TriggerActionConfig.Broadcast -> handleBroadcast(trigger, action)
            is TriggerActionConfig.Notification -> handleNotification(trigger, action)
        }
    }

    private suspend fun handleBroadcast(
        trigger: ParsedCampaignTrigger,
        action: TriggerActionConfig.Broadcast
    ) {
        // Trigger conditions are only evaluated on the watch, but broadcast intents are only
        // meaningful on the phone (no receiver apps run on Wear OS). Forward the broadcast
        // payload to the phone via BLE so it can call context.sendBroadcast() there.
        //
        // The OPEN_WEBAPP action is still forwarded via the dedicated WEBAPP_TRIGGER key for
        // backwards compatibility with the phone-side BLEHelper handler.
        if (action.action == Constants.Trigger.ACTION_OPEN_WEBAPP) {
            forwardWebAppTriggerToPhone(trigger, action)
            return
        }

        Log.d(TAG, "Forwarding broadcast to phone via BLE: action=${action.action}")
        try {
            val extrasJson = buildJsonObject {
                action.extras.forEach { extra ->
                    put(extra.key, extra.value)
                }
            }
            val payload = buildJsonObject {
                put("action", action.action)
                put("extras", extrasJson)
                put("timestamp", System.currentTimeMillis())
            }.toString()
            bleChannel.send(Constants.BLE.KEY_BROADCAST_TRIGGER, payload)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to forward broadcast trigger: ${e.message}", e)
        }
    }

    /**
     * Forwards an [Constants.Trigger.ACTION_OPEN_WEBAPP] broadcast's `survey_id`/`webapp_id`
     * extras to the phone, where [kaist.iclab.mobiletracker.helpers.BLEHelper] picks it up and
     * calls into `WebAppTriggerHandler.launch(...)`.
     */
    private suspend fun forwardWebAppTriggerToPhone(
        trigger: ParsedCampaignTrigger,
        action: TriggerActionConfig.Broadcast
    ) {
        val surveyId = action.extras.firstOrNull { it.key == "survey_id" }?.value
        val webAppId = action.extras.firstOrNull { it.key == "webapp_id" }?.value
        if (surveyId == null || webAppId == null) {
            Log.e(TAG, "OPEN_WEBAPP broadcast missing survey_id/webapp_id extras (trigger: ${trigger.name})")
            return
        }

        try {
            val payload = buildJsonObject {
                put("survey_id", surveyId)
                put("webapp_id", webAppId)
                put("timestamp", System.currentTimeMillis())
            }.toString()
            bleChannel.send(Constants.BLE.KEY_WEBAPP_TRIGGER, payload)
            Log.d(TAG, "Forwarded webapp trigger to phone: $payload")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to forward webapp trigger for trigger ${trigger.name}: ${e.message}", e)
        }
    }

    private suspend fun handleNotification(
        trigger: ParsedCampaignTrigger,
        action: TriggerActionConfig.Notification
    ) {
        Log.d(TAG, "Triggering Notification: title=${action.title}, url=${action.url}, trigger=${trigger.name}")
        try {
            val payload = buildJsonObject {
                put("title", action.title)
                put("body", action.description)
                put("url", action.url)
                put("timestamp", System.currentTimeMillis())
            }.toString()
            bleChannel.send(Constants.BLE.KEY_NOTIFICATION_TRIGGER, payload)
            Log.d(TAG, "Forwarded notification trigger to phone: $payload")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to forward notification trigger: ${e.message}", e)
        }
    }

    private suspend fun handleEma(
        trigger: ParsedCampaignTrigger,
        action: TriggerActionConfig.Ema
    ) {
        Log.d(TAG, "Triggering Phone EMA: surveyId=${action.surveyId}, trigger=${trigger.name}")
        try {
            bleChannel.send(Constants.BLE.KEY_PHONE_EMA_TRIGGER, action.surveyId.toString())
            Log.d(TAG, "Sent phone EMA trigger via BLE")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send phone EMA trigger: ${e.message}", e)
        }
    }

    private fun handleWatchEma(
        trigger: ParsedCampaignTrigger,
        action: TriggerActionConfig.WatchEma
    ) {
        val config = surveyConfigs[action.surveyId]
        if (config == null) {
            Log.w(
                TAG,
                "No survey config found for surveyId=${action.surveyId} " +
                        "(trigger: ${trigger.name}). Available IDs: ${surveyConfigs.keys}"
            )
            return
        }

        Log.d(TAG, "Triggering MicroEMA: surveyId=${action.surveyId}, trigger=${trigger.name}")

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
