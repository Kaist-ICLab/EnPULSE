package kaist.iclab.mobiletracker.data.campaign

import kotlinx.serialization.Serializable

/**
 * Data class representing a campaign_webapp entry from Supabase.
 */
@Serializable
data class CampaignWebAppData(
    val id: Long,
    val campaign_id: Long,
    val name: String,
    val url: String,

    /** Path of the uploaded icon within the `campaign-webapp-icons` Storage bucket, if set. */
    val icon_path: String? = null
)
