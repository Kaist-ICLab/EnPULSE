package kaist.iclab.mobiletracker.services

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kaist.iclab.mobiletracker.data.campaign.CampaignWebAppData
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.repository.Result
import kaist.iclab.mobiletracker.repository.ErrorClassifier
import kaist.iclab.mobiletracker.utils.SupabaseLoadingInterceptor

/**
 * Service for handling WebApp data operations with Supabase.
 */
class WebAppService(
    private val supabaseHelper: SupabaseHelper
) {
    private val supabaseClient = supabaseHelper.supabaseClient
    private val tableName = "campaign_webapp"
    private val iconBucket = "campaign-webapp-icons"

    companion object {
        private const val TAG = "WebAppService"
    }

    /**
     * Fetch all webapps for a specific campaign
     */
    suspend fun getCampaignWebApps(campaignId: Int): Result<List<CampaignWebAppData>> {
        return SupabaseLoadingInterceptor.withLoading {
            ErrorClassifier.runClassified(TAG, "getCampaignWebApps") {
                supabaseClient.from(tableName)
                    .select {
                        filter {
                            eq("campaign_id", campaignId)
                        }
                    }
                    .decodeList<CampaignWebAppData>()
            }
        }
    }

    /**
     * Downloads a webapp icon's raw bytes from the fixed `campaign-webapp-icons` Storage bucket.
     * @param iconPath Object path within the bucket, as stored in `campaign_webapp.icon_path`.
     */
    suspend fun downloadIcon(iconPath: String): Result<ByteArray> {
        return ErrorClassifier.runClassified(TAG, "downloadIcon") {
            supabaseClient.storage.from(iconBucket).downloadAuthenticated(iconPath)
        }
    }
}
