package kaist.iclab.wearabletracker.trigger

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import kaist.iclab.tracker.sensor.microema.WatchSurveyConfig
import kaist.iclab.tracker.trigger.engine.TriggerActionHandler
import kaist.iclab.tracker.trigger.model.ParsedCampaignTrigger
import kaist.iclab.tracker.trigger.model.TriggerActionConfig
import kaist.iclab.wearabletracker.Constants
import kaist.iclab.wearabletracker.R
import kaist.iclab.wearabletracker.ema.MicroEmaRepository
import kaist.iclab.wearabletracker.ema.WatchSurveyActivity
import kaist.iclab.wearabletracker.helpers.NotificationHelper

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
    private val microEmaRepository: MicroEmaRepository
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
            is TriggerActionConfig.Ema -> {
                Log.d(TAG, "Phone EMA action not yet implemented (trigger: ${trigger.name})")
            }

            is TriggerActionConfig.Broadcast -> handleBroadcast(trigger, action)
        }
    }

    private fun handleBroadcast(
        trigger: ParsedCampaignTrigger,
        action: TriggerActionConfig.Broadcast
    ) {
        Log.d(TAG, "Triggering Broadcast: action=${action.action}, trigger=${trigger.name}")
        try {
            val intent = Intent(action.action)

            // Note: If you want to make this an Explicit Intent in the future (targeting a specific app),
            // you can add a 'packageName' field to the Supabase payload and call intent.setPackage(packageName).

            // Attach all configured extras based on their specified type
            for (extra in action.extras) {
                when (extra.valueType.lowercase()) {
                    "int", "integer" -> intent.putExtra(extra.key, extra.value.toIntOrNull() ?: 0)
                    "long" -> intent.putExtra(extra.key, extra.value.toLongOrNull() ?: 0L)
                    "float" -> intent.putExtra(extra.key, extra.value.toFloatOrNull() ?: 0f)
                    "double" -> intent.putExtra(extra.key, extra.value.toDoubleOrNull() ?: 0.0)
                    "bool", "boolean" -> intent.putExtra(extra.key, extra.value.toBoolean())
                    else -> intent.putExtra(extra.key, extra.value) // Default to String
                }
            }

            // Send the broadcast to the Android system
            context.sendBroadcast(intent)
            Log.d(TAG, "Broadcast sent successfully: ${action.action}")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to send broadcast for trigger ${trigger.name}: ${e.message}", e)
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
        launchMicroEma()
    }

    /**
     * Vibrate and launch the MicroEMA survey activity using a High-Priority Notification
     * with a Full-Screen Intent. This bypasses background activity launch restrictions.
     */
    private fun launchMicroEma() {
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
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            NotificationHelper.ensureNotificationChannel(
                context,
                NotificationHelper.NotificationChannelConfig.TRIGGER
            )

            val notification = NotificationCompat.Builder(
                context,
                Constants.NotificationChannel.TRIGGER_ID
            )
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(context.getString(R.string.notification_trigger_title))
                .setContentText(context.getString(R.string.notification_trigger_text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(pendingIntent, true)
                .setAutoCancel(true)
                .build()

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(Constants.NotificationId.TRIGGER, notification)

            Log.d(TAG, "WatchSurveyActivity launched via Full-Screen Intent Notification")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch WatchSurveyActivity: ${e.message}", e)
        }
    }
}
