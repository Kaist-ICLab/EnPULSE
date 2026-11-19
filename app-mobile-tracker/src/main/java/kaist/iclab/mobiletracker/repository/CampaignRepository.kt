package kaist.iclab.mobiletracker.repository

import kaist.iclab.mobiletracker.data.campaign.CampaignData
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository for managing campaign data.
 * Centralizes campaign fetching and caching.
 */
interface CampaignRepository {
    /**
     * Observable flow of cached campaigns
     */
    val campaignsFlow: StateFlow<List<CampaignData>>

    /**
     * Fetch all campaigns from remote source.
     * Updates the campaignsFlow on success.
     */
    suspend fun fetchCampaigns(): Result<List<CampaignData>>

    /**
     * Join a campaign
     * @param campaignId The ID of the campaign
     * @param password The password to join the campaign
     */
    suspend fun joinCampaign(campaignId: String, password: String): Result<Boolean>
}
