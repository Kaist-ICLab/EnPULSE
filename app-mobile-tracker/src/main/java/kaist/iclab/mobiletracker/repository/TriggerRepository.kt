package kaist.iclab.mobiletracker.repository

import kaist.iclab.mobiletracker.data.trigger.CampaignTriggerList
import kaist.iclab.mobiletracker.services.TriggerService
import kaist.iclab.mobiletracker.storage.CouchbaseTriggerConfigStorage
import kotlinx.coroutines.flow.StateFlow

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

    override val triggersFlow: StateFlow<CampaignTriggerList>
        get() = persistentStorage.stateFlow

    override fun getCachedTriggers(): CampaignTriggerList = persistentStorage.get()

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
