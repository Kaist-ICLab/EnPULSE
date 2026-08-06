package kaist.iclab.wearabletracker.trigger

import android.util.Log
import kaist.iclab.tracker.sensor.microema.WatchSurveyConfig
import kaist.iclab.tracker.sync.ble.BLEDataChannel
import kaist.iclab.wearabletracker.Constants
import kaist.iclab.wearabletracker.ema.MicroEmaRepository
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

/**
 * Receives watch survey (question content) configuration from the phone via BLE.
 *
 * Renamed/slimmed from `TriggerConfigReceiver` — the trigger engine now lives entirely on the
 * phone, so this no longer loads `ParsedCampaignTrigger`/condition data into a local engine (there
 * isn't one anymore). It only caches [WatchSurveyConfig]s, keyed by survey id, so
 * [WatchEmaTriggerReceiver] can look them up when the phone tells the watch (via
 * `KEY_WATCH_EMA_TRIGGER`) which survey to launch.
 *
 * ## BLE Payload Format
 *
 * The phone sends a JSON payload on the `trigger_config` BLE key:
 * ```json
 * { "surveyConfigs": { "34": { ... WatchSurveyConfig ... } } }
 * ```
 */
class WatchSurveyConfigReceiver(
    private val bleChannel: BLEDataChannel,
    private val microEmaRepository: MicroEmaRepository,
    private val storage: WatchSurveyConfigStorage
) {
    companion object {
        private const val TAG = "WatchSurveyConfigRcvr"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private var isListening = false

    /** Survey configurations keyed by survey ID, kept in sync with the phone. */
    private var surveyConfigs: Map<Int, WatchSurveyConfig> = emptyMap()

    fun getSurveyConfig(surveyId: Int): WatchSurveyConfig? = surveyConfigs[surveyId]

    /**
     * Start listening for watch survey config messages from the phone.
     * Idempotent — safe to call multiple times.
     */
    fun startListening() {
        if (isListening) return
        isListening = true

        bleChannel.addOnReceivedListener(
            setOf(Constants.BLE.KEY_TRIGGER_CONFIG)
        ) { _, jsonElement ->
            handleConfig(jsonElement)
        }

        Log.d(TAG, "Started listening for watch survey config on BLE key: ${Constants.BLE.KEY_TRIGGER_CONFIG}")

        // Load persisted configuration on startup if it exists
        try {
            val cachedConfig = storage.loadConfig()
            if (cachedConfig != null) {
                Log.d(TAG, "Loading cached watch survey config from disk: ${cachedConfig.surveyConfigs.size} survey(s)")
                applyConfig(cachedConfig.surveyConfigs)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load cached watch survey config: ${e.message}", e)
        }
    }

    private fun handleConfig(jsonElement: JsonElement) {
        try {
            Log.d(TAG, "Received watch survey config payload")

            val payload = when (jsonElement) {
                is kotlinx.serialization.json.JsonObject -> jsonElement
                is kotlinx.serialization.json.JsonPrimitive -> json.parseToJsonElement(jsonElement.content).jsonObject
                else -> json.parseToJsonElement(jsonElement.toString()).jsonObject
            }

            val configPayload = json.decodeFromJsonElement<WatchSurveyConfigPayload>(payload)

            Log.d(TAG, "Parsed ${configPayload.surveyConfigs.size} survey config(s)")

            applyConfig(configPayload.surveyConfigs)
            storage.saveConfig(configPayload)

            Log.d(TAG, "Watch survey config applied and persisted successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing watch survey config payload: ${e.message}", e)
        }
    }

    private fun applyConfig(configs: Map<Int, WatchSurveyConfig>) {
        surveyConfigs = configs

        // Reset the queue progress for these surveys so they start from Q1
        configs.keys.forEach { surveyId ->
            microEmaRepository.resetQueueProgress(surveyId)
        }

        Log.d(TAG, "Updated survey configs and reset queue progress: ${configs.keys}")
    }
}

/**
 * BLE payload for watch survey configuration sent from phone to watch.
 */
@Serializable
data class WatchSurveyConfigPayload(
    val surveyConfigs: Map<Int, WatchSurveyConfig>
)
