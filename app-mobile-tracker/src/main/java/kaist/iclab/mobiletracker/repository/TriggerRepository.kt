package kaist.iclab.mobiletracker.repository

import android.util.Log
import kaist.iclab.mobiletracker.data.trigger.CampaignTriggerEntity
import kaist.iclab.mobiletracker.data.trigger.CampaignTriggerList
import kaist.iclab.mobiletracker.services.TriggerService
import kaist.iclab.mobiletracker.storage.CouchbaseTriggerConfigStorage
import kaist.iclab.tracker.trigger.model.ConditionNode
import kaist.iclab.tracker.trigger.model.ParsedCampaignTrigger
import kaist.iclab.tracker.trigger.model.TriggerActionConfig
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * Repository for managing campaign trigger configurations.
 */
interface TriggerRepository {
    /**
     * Observable flow of locally persisted trigger configurations.
     */
    val triggersFlow: StateFlow<CampaignTriggerList>

    /**
     * Get the current trigger configurations directly from persisted storage.
     */
    fun getCachedTriggers(): CampaignTriggerList

    /**
     * Parse the currently cached triggers into runtime [ParsedCampaignTrigger]s, ready to load
     * into a [kaist.iclab.tracker.trigger.engine.TriggerEngine]. Entries that fail to parse
     * (malformed condition/action JSON) are skipped and logged, not thrown.
     */
    fun getParsedTriggers(): List<ParsedCampaignTrigger>

    /**
     * Fetch triggers for a campaign from Supabase and persist locally.
     * @param campaignId The campaign ID to fetch triggers for.
     * @return Result containing the count of triggers fetched, or error.
     */
    suspend fun fetchAndPersistTriggers(campaignId: Int): Result<Int>

    /**
     * Clear all locally persisted trigger configurations.
     */
    fun clearTriggers()
}

class TriggerRepositoryImpl(
    private val triggerService: TriggerService,
    private val persistentStorage: CouchbaseTriggerConfigStorage
) : TriggerRepository {
    companion object {
        private const val TAG = "TriggerRepo"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override val triggersFlow: StateFlow<CampaignTriggerList>
        get() = persistentStorage.stateFlow

    override fun getCachedTriggers(): CampaignTriggerList = persistentStorage.get()

    override fun getParsedTriggers(): List<ParsedCampaignTrigger> {
        return getCachedTriggers().triggers.mapNotNull { entity ->
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
    }

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

    override suspend fun fetchAndPersistTriggers(campaignId: Int): Result<Int> {
        return ErrorClassifier.runClassified(TAG, "fetch and persist triggers") {
            when (val result = triggerService.getCampaignTriggers(campaignId)) {
                is Result.Success -> {
                    val triggers = result.data
                    persistentStorage.set(CampaignTriggerList(triggers))
                    triggers.size
                }

                is Result.Error -> {
                    throw result.exception
                }
            }
        }
    }

    override fun clearTriggers() {
        persistentStorage.set(CampaignTriggerList())
    }
}
