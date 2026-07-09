package kaist.iclab.mobiletracker.data.sensors

import kotlinx.serialization.Serializable

/**
 * Supabase data class representing campaign data.
 *
 * @property id Unique identifier for the campaign entry (integer primary key).
 * @property name Name of the campaign.
 */
@Serializable
data class CampaignData(
    val id: Int? = null,
    val name: String
)

