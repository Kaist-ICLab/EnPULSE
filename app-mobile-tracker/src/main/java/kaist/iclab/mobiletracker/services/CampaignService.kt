package kaist.iclab.mobiletracker.services

import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kaist.iclab.mobiletracker.data.campaign.CampaignData
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.repository.AppError
import kaist.iclab.mobiletracker.repository.ErrorClassifier
import kaist.iclab.mobiletracker.repository.Result
import kaist.iclab.mobiletracker.repository.flatMap
import kaist.iclab.mobiletracker.utils.SupabaseLoadingInterceptor
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Service for handling campaign data operations with Supabase
 */
class CampaignService(
    supabaseHelper: SupabaseHelper
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
            ErrorClassifier.runClassified(TAG, "getAllCampaigns") {
                supabaseClient.from(tableName).select().decodeList<CampaignData>()
            }
        }
    }

    /**
     * Fetch a single campaign by ID
     * @param campaignId The ID of the campaign to fetch (as String)
     * @return Result containing the campaign or error
     */
    suspend fun getCampaignById(campaignId: String): Result<CampaignData> {
        val campaignIdInt = campaignId.toIntOrNull()
            ?: return Result.Error(
                AppError.Validation("Invalid campaign ID format: $campaignId")
            )

        return getAllCampaigns().flatMap { campaigns ->
            val campaign = campaigns.find { it.id == campaignIdInt }
            if (campaign != null) {
                Result.Success(campaign)
            } else {
                Result.Error(AppError.NotFound("Campaign with ID $campaignId not found"))
            }
        }
    }

    /**
     * Join a campaign by invoking the 'join-campaign' edge function.
     *
     * @param campaignId The ID of the campaign
     * @param password The raw password to verify (if required)
     * @return Result containing boolean (true if joined successfully) or error
     */
    suspend fun joinCampaign(campaignId: String, password: String): Result<Boolean> {
        val campaignIdInt = campaignId.toIntOrNull()
            ?: return Result.Error(
                AppError.Validation("Invalid campaign ID format: $campaignId")
            )

        return SupabaseLoadingInterceptor.withLoading {
            ErrorClassifier.runClassified(TAG, "joinCampaign") {
                val body = buildJsonObject {
                    put("campaignId", campaignIdInt)
                    put("password", password)
                }
                supabaseClient.functions.invoke(
                    function = "join-campaign",
                    body = body,
                    headers = Headers.build {
                        append(HttpHeaders.ContentType, "application/json")
                    }
                )
                true
            }
        }
    }
}
