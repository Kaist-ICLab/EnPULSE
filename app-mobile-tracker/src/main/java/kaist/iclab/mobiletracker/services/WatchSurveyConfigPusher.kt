package kaist.iclab.mobiletracker.services

import android.util.Log
import kaist.iclab.mobiletracker.config.AppConfig
import kaist.iclab.tracker.sensor.microema.MicroEmaBuilder
import kaist.iclab.tracker.sensor.microema.WatchSurveyConfig
import kaist.iclab.tracker.sensor.survey.config.SurveyConfig
import kaist.iclab.tracker.sync.ble.BLEDataChannel
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Responsible for pushing watch MicroEMA survey (question content) configs to the watch via BLE
 * after fetching from Supabase.
 *
 * The trigger engine now lives entirely on the phone (see `di/phone/TriggerModule.kt`), so unlike
 * its predecessor `TriggerConfigPusher`, this no longer pushes `ParsedCampaignTrigger`/condition
 * data to the watch at all — the watch never evaluates trigger conditions anymore, it only needs
 * the survey question content so it can render `WatchSurveyActivity` once told (over BLE, via
 * `KEY_WATCH_EMA_TRIGGER`) which survey to launch.
 *
 * ## Flow
 * 1. Phone fetches `survey` configs from Supabase (already done by `SurveyRepositoryImpl`).
 * 2. This class filters to watch (deviceType=1) surveys, builds [WatchSurveyConfig]s.
 * 3. Serializes them into a [WatchSurveyConfigPayload] and sends via BLE.
 * 4. Watch's `WatchSurveyConfigReceiver` receives, parses, and caches them for later playback.
 */
class WatchSurveyConfigPusher(
    private val bleChannel: BLEDataChannel
) {
    companion object {
        private const val TAG = "WatchSurveyConfigPusher"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    /**
     * Filter watch survey configs and send them to the watch via BLE.
     *
     * @param surveyConfigs Survey configs fetched from Supabase (only deviceType=1 with a
     *   non-null expireAfterMs are sent — the same filter `TriggerConfigPusher` used to apply).
     */
    suspend fun pushToWatch(surveyConfigs: List<SurveyConfig>) {
        try {
            val watchSurveyConfigs = surveyConfigs
                .filter { it.deviceType == 1 && it.expireAfterMs != null }
                .associate { survey ->
                    val watchConfig = MicroEmaBuilder.build(survey)
                    watchConfig.surveyId to watchConfig
                }

            if (watchSurveyConfigs.isEmpty()) {
                Log.w(TAG, "No watch survey configs to push")
                return
            }

            val payload = WatchSurveyConfigPayload(surveyConfigs = watchSurveyConfigs)
            val payloadJson = json.encodeToString(WatchSurveyConfigPayload.serializer(), payload)

            Log.d(TAG, "Pushing ${watchSurveyConfigs.size} watch survey config(s) to watch")

            bleChannel.send(
                AppConfig.BLEKeys.TRIGGER_CONFIG,
                payloadJson,
                isUrgent = true
            )

            Log.d(TAG, "Watch survey config pushed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to push watch survey config: ${e.message}", e)
        }
    }
}

/**
 * BLE payload for watch survey configuration sent from phone to watch.
 */
@Serializable
data class WatchSurveyConfigPayload(
    val surveyConfigs: Map<Int, WatchSurveyConfig>
)
