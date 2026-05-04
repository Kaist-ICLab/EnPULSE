package kaist.iclab.wearabletracker.trigger

import android.content.Context
import android.content.Intent
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kaist.iclab.tracker.sensor.microema.WatchSurveyConfig
import kaist.iclab.tracker.trigger.engine.TriggerActionHandler
import kaist.iclab.tracker.trigger.model.ParsedCampaignTrigger
import kaist.iclab.tracker.trigger.model.TriggerActionConfig
import kaist.iclab.wearabletracker.ema.MicroEmaRepository
import kaist.iclab.wearabletracker.ema.WatchSurveyActivity

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
        Log.d(TAG, "Updated survey configs: ${configs.keys}")
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
            is TriggerActionConfig.Broadcast -> {
                Log.d(TAG, "Broadcast action not yet implemented (trigger: ${trigger.name})")
            }
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
     * Vibrate and launch the MicroEMA survey activity.
     * Same behavior as [MicroEmaResponseManager.launchMicroEma].
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
            context.startActivity(intent)

            Log.d(TAG, "WatchSurveyActivity launched")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch WatchSurveyActivity: ${e.message}", e)
        }
    }
}
