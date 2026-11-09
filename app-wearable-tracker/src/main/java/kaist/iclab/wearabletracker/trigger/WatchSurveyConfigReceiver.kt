package kaist.iclab.wearabletracker.trigger

import android.util.Log
import kaist.iclab.tracker.sensor.microema.WatchSurveyConfig
import kaist.iclab.tracker.sync.ble.BLEDataChannel
import kaist.iclab.wearabletracker.Constants
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

    /** [WatchSurveyConfigPayload.pushedAt] of the config currently applied. */
    private var appliedPushedAt = 0L

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

        // Load persisted configuration on startup if it exists
        try {
            val cachedConfig = storage.loadConfig()
            synchronized(this) {
                // The listener above is already registered, so a fresh push may have been applied by now.
                if (cachedConfig != null && (appliedPushedAt == 0L || cachedConfig.pushedAt >= appliedPushedAt)) {
                    applyConfig(cachedConfig.surveyConfigs)
                    appliedPushedAt = cachedConfig.pushedAt
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load cached watch survey config: ${e.message}", e)
        }
    }

    @Synchronized
    private fun handleConfig(jsonElement: JsonElement) {
        try {
            val payload = when (jsonElement) {
                is kotlinx.serialization.json.JsonObject -> jsonElement
                is kotlinx.serialization.json.JsonPrimitive -> json.parseToJsonElement(jsonElement.content).jsonObject
                else -> json.parseToJsonElement(jsonElement.toString()).jsonObject
            }

            val configPayload = json.decodeFromJsonElement<WatchSurveyConfigPayload>(payload)

            // Each push is its own Data Layer item, so pushes queued while the watch was away all
            // arrive on reconnect in no fixed order. Keep the newest; 0 means the phone didn't say.
            if (configPayload.pushedAt != 0L && configPayload.pushedAt < appliedPushedAt) {
                Log.w(
                    TAG,
                    "Ignoring an older watch survey config (pushed at ${configPayload.pushedAt}, " +
                        "already applied one from $appliedPushedAt)"
                )
                return
            }

            applyConfig(configPayload.surveyConfigs)
            appliedPushedAt = configPayload.pushedAt
            storage.saveConfig(configPayload)

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing watch survey config payload: ${e.message}", e)
        }
    }

    private fun applyConfig(configs: Map<Int, WatchSurveyConfig>) {
        // Question position is kept across pushes and restarts: resetting it here meant later
        // questions were rarely asked.
        surveyConfigs = configs
    }
}

/**
 * BLE payload for watch survey configuration sent from phone to watch.
 */
@Serializable
data class WatchSurveyConfigPayload(
    val surveyConfigs: Map<Int, WatchSurveyConfig>,
    /** When the phone sent this push; 0 if unknown. See [WatchSurveyConfigReceiver.handleConfig]. */
    val pushedAt: Long = 0L
)
