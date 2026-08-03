package kaist.iclab.mobiletracker.services

import io.github.jan.supabase.postgrest.from
import kaist.iclab.mobiletracker.data.trigger.CampaignTriggerEntity
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.repository.ErrorClassifier
import kaist.iclab.mobiletracker.repository.Result
import kaist.iclab.mobiletracker.utils.SupabaseLoadingInterceptor

/**
 * Service for handling campaign trigger operations with Supabase.
 */
class TriggerService(
    supabaseHelper: SupabaseHelper
) {
    private val supabaseClient = supabaseHelper.supabaseClient
    private val tableName = "campaign_trigger"

    companion object {
        private const val TAG = "TriggerService"
    }

    /**
     * Fetch all triggers for a specific campaign.
     * @param campaignId Unique ID of the campaign.
     * @return Result containing list of triggers or error.
     */
    suspend fun getCampaignTriggers(campaignId: Int): Result<List<CampaignTriggerEntity>> {
        return SupabaseLoadingInterceptor.withLoading {
            ErrorClassifier.runClassified(TAG, "getCampaignTriggers($campaignId)") {
                supabaseClient.from(tableName)
                    .select {
                        filter {
                            eq("campaign_id", campaignId)
                        }
                    }
                    .decodeList<CampaignTriggerEntity>()
            }
        }
    }
}
