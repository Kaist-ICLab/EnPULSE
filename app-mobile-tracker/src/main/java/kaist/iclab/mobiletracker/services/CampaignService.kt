package kaist.iclab.mobiletracker.services

import android.util.Log
import io.github.jan.supabase.postgrest.from
import kaist.iclab.mobiletracker.data.campaign.CampaignData
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.repository.Result
import kaist.iclab.mobiletracker.repository.runCatchingSuspend
import kaist.iclab.mobiletracker.utils.SupabaseLoadingInterceptor

/**
 * Service for handling campaign data operations with Supabase
 */
class CampaignService(
    private val supabaseHelper: SupabaseHelper
) {
    private val supabaseClient = supabaseHelper.supabaseClient
    private val tableName = "campaigns"

    companion object {
        private const val TAG = "CampaignService"
    }

    /**
     * Fetch all campaigns from Supabase
     * @return Result containing list of campaigns or error
     */
    suspend fun getAllCampaigns(): Result<List<CampaignData>> {
        return SupabaseLoadingInterceptor.withLoading {
            runCatchingSuspend {
                try {
                    val response = supabaseClient.from(tableName).select()
                    val campaigns = response.decodeList<CampaignData>()
                    campaigns
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching campaigns from Supabase: ${e.message}", e)
                    throw e
                }
            }
        }
    }

    /**
     * Fetch a single campaign by ID
     * @param campaignId The ID of the campaign to fetch (as String)
     * @return Result containing the campaign or error
     */
    suspend fun getCampaignById(campaignId: String): Result<CampaignData> {
        return runCatchingSuspend {
            try {
                val campaignIdInt = campaignId.toIntOrNull()
                    ?: throw IllegalArgumentException("Invalid campaign ID format: $campaignId")

                val allCampaignsResult = getAllCampaigns()
                val allCampaigns = when (allCampaignsResult) {
                    is Result.Success -> allCampaignsResult.data
                    is Result.Error -> throw allCampaignsResult.exception
                }
                val campaign = allCampaigns.find { it.id == campaignIdInt }
                if (campaign == null) {
                    val error = NoSuchElementException("Campaign with ID $campaignId not found")
                    Log.e(TAG, "Campaign not found: $campaignId", error)
                    throw error
                }
                campaign
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching campaign by ID ($campaignId): ${e.message}", e)
                throw e
            }
        }
    }
}

