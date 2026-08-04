package kaist.iclab.mobiletracker.repository

import kaist.iclab.mobiletracker.data.campaign.WebAppConfigList
import kotlinx.coroutines.flow.StateFlow

interface WebAppRepository {
    val webAppsFlow: StateFlow<WebAppConfigList>

    /**
     * Fetches webapps for a given campaign from Supabase and persists them to Couchbase.
     * @param campaignId The ID of the current campaign.
     * @return Result containing the number of webapps fetched and persisted, or an error.
     */
    suspend fun fetchAndPersistWebApps(campaignId: Int): Result<Int>

    /**
     * Clears local webapp configurations.
     */
    fun clearWebApps()
}
