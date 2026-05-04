package kaist.iclab.wearabletracker.trigger

import android.util.Log
import kaist.iclab.tracker.sensor.microema.WatchSurveyConfig
import kaist.iclab.tracker.sync.ble.BLEDataChannel
import kaist.iclab.tracker.trigger.engine.TriggerEngine
import kaist.iclab.tracker.trigger.model.ParsedCampaignTrigger
import kaist.iclab.wearabletracker.Constants
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

/**
 * Receives trigger configuration from the phone via BLE and initializes
 * the trigger engine with parsed trigger rules and survey configs.
 *
 * ## BLE Payload Format
 *
 * The phone sends a JSON payload on the `trigger_config` BLE key:
 * ```json
 * {
 *   "triggers": [
 *     {
 *       "id": 5,
 *       "campaignId": 1,
 *       "name": "Trigger X",
 *       "condition": { "type": "and", "children": [...] },
 *       "actions": [ { "kind": "watch_ema", "surveyId": 34, "minIntervalMillis": 50000 } ]
 *     }
 *   ],
 *   "surveyConfigs": {
 *     "34": { ... WatchSurveyConfig ... }
 *   }
 * }
 * ```
 */
class TriggerConfigReceiver(
    private val bleChannel: BLEDataChannel,
    private val triggerEngine: TriggerEngine,
    private val actionHandler: WatchTriggerActionHandler
) {
    companion object {
        private const val TAG = "TriggerConfigRcvr"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private var isListening = false

    /**
     * Start listening for trigger config messages from the phone.
     * Idempotent — safe to call multiple times.
     */
    fun startListening() {
        if (isListening) return
        isListening = true

        bleChannel.addOnReceivedListener(
            setOf(Constants.BLE.KEY_TRIGGER_CONFIG)
        ) { _, jsonElement ->
            handleTriggerConfig(jsonElement)
        }

        Log.d(
            TAG,
            "Started listening for trigger config on BLE key: ${Constants.BLE.KEY_TRIGGER_CONFIG}"
        )
    }

    private fun handleTriggerConfig(jsonElement: JsonElement) {
        try {
            Log.d(TAG, "Received trigger config payload")

            val payload = when (jsonElement) {
                is kotlinx.serialization.json.JsonObject -> jsonElement
                is kotlinx.serialization.json.JsonPrimitive -> {
                    json.parseToJsonElement(jsonElement.content).jsonObject
                }

                else -> {
                    json.parseToJsonElement(jsonElement.toString()).jsonObject
                }
            }

            // Parse the payload
            val configPayload = json.decodeFromJsonElement<TriggerConfigPayload>(payload)

            Log.d(
                TAG,
                "Parsed ${configPayload.triggers.size} trigger(s) and ${configPayload.surveyConfigs.size} survey config(s)"
            )

            // Update survey configs in the action handler
            actionHandler.updateSurveyConfigs(configPayload.surveyConfigs)

            // Load triggers into the engine
            triggerEngine.loadTriggers(configPayload.triggers)

            Log.d(TAG, "Trigger engine configured successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing trigger config payload: ${e.message}", e)
        }
    }
}

/**
 * BLE payload structure for trigger configuration.
 *
 * Sent from the phone after fetching trigger and survey configs from Supabase.
 */
@Serializable
data class TriggerConfigPayload(
    val triggers: List<ParsedCampaignTrigger>,
    val surveyConfigs: Map<Int, WatchSurveyConfig>
)
