package kaist.iclab.mobiletracker.services

import android.util.Log
import kaist.iclab.mobiletracker.config.AppConfig
import kaist.iclab.mobiletracker.data.trigger.CampaignTriggerEntity
import kaist.iclab.tracker.sensor.microema.MicroEmaBuilder
import kaist.iclab.tracker.sensor.microema.WatchSurveyConfig
import kaist.iclab.tracker.sensor.survey.config.SurveyConfig
import kaist.iclab.tracker.sync.ble.BLEDataChannel
import kaist.iclab.tracker.trigger.model.ConditionNode
import kaist.iclab.tracker.trigger.model.ParsedCampaignTrigger
import kaist.iclab.tracker.trigger.model.TriggerActionConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * Responsible for pushing trigger configurations and associated survey configs
 * to the watch via BLE after fetching from Supabase.
 *
 * ## Flow
 * 1. Phone fetches `campaign_trigger` entities from Supabase (raw JSON)
 * 2. Phone fetches `campaign_survey` configs from Supabase
 * 3. This class parses the raw JSON into [ParsedCampaignTrigger] and [WatchSurveyConfig]
 * 4. Serializes them into a [TriggerConfigPayload] and sends via BLE
 * 5. Watch's [TriggerConfigReceiver] receives, parses, and feeds into the engine
 */
class TriggerConfigPusher(
    private val bleChannel: BLEDataChannel
) {
    companion object {
        private const val TAG = "TriggerConfigPusher"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    /**
     * Parse raw trigger entities and survey configs, then send to watch via BLE.
     *
     * @param triggerEntities Raw trigger entities from Supabase (with JSON condition/action fields).
     * @param surveyConfigs Survey configs for watch (deviceType=1) surveys.
     */
    suspend fun pushToWatch(
        triggerEntities: List<CampaignTriggerEntity>,
        surveyConfigs: List<SurveyConfig>
    ) {
        try {
            // 1. Parse raw trigger entities into typed models
            val parsedTriggers = triggerEntities.mapNotNull { entity ->
                try {
                    parseTriggerEntity(entity)
                } catch (e: Exception) {
                    Log.e(
                        TAG,
                        "Failed to parse trigger '${entity.name}' (id=${entity.id}): ${e.message}",
                        e
                    )
                    null
                }
            }

            if (parsedTriggers.isEmpty()) {
                Log.w(TAG, "No valid triggers to push to watch")
                return
            }

            // 2. Build watch survey config map
            val watchSurveyConfigs = surveyConfigs
                .filter { it.deviceType == 1 && it.expireAfterMs != null }
                .associate { survey ->
                    val watchConfig = MicroEmaBuilder.build(survey)
                    watchConfig.surveyId to watchConfig
                }

            // 3. Build and send payload
            val payload = TriggerConfigPayload(
                triggers = parsedTriggers,
                surveyConfigs = watchSurveyConfigs
            )

            val payloadJson = json.encodeToString(TriggerConfigPayload.serializer(), payload)
            Log.d(
                TAG,
                "Pushing trigger config to watch: ${parsedTriggers.size} trigger(s), ${watchSurveyConfigs.size} survey config(s)"
            )

            bleChannel.send(
                AppConfig.BLEKeys.TRIGGER_CONFIG,
                payloadJson,
                isUrgent = true
            )

            Log.d(TAG, "Trigger config pushed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to push trigger config to watch: ${e.message}", e)
        }
    }

    /**
     * Parse a single [CampaignTriggerEntity] into a [ParsedCampaignTrigger].
     */
    private fun parseTriggerEntity(entity: CampaignTriggerEntity): ParsedCampaignTrigger {
        val condition: ConditionNode = json.decodeFromJsonElement(entity.condition)
        val actions: List<TriggerActionConfig> = json.decodeFromJsonElement(entity.action)

        return ParsedCampaignTrigger(
            id = entity.id,
            campaignId = entity.campaignId,
            name = entity.name,
            condition = condition,
            actions = actions
        )
    }
}

/**
 * BLE payload for trigger configuration sent from phone to watch.
 */
@Serializable
data class TriggerConfigPayload(
    val triggers: List<ParsedCampaignTrigger>,
    val surveyConfigs: Map<Int, WatchSurveyConfig>
)
