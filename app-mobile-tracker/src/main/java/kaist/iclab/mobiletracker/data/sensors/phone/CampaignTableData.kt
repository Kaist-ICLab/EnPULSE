package kaist.iclab.mobiletracker.data.sensors.phone

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Supabase data class representing campaign table data.
 *
 * @property id Unique identifier for the campaign table entry (integer primary key).
 * @property campaignId Campaign identifier.
 * @property name Name of the campaign table.
 * @property description Description of the campaign table.
 * @property dailyCountMax Maximum daily count allowed for the table.
 */
@Serializable
data class CampaignTableData(
    val id: Long? = null,
    @SerialName("campaign_id")
    val campaignId: Long,
    val name: String,
    val description: String? = null,
    @SerialName("daily_count_max")
    val dailyCountMax: Int? = null,
    @SerialName("is_custom")
    val isCustom: Boolean,
    @SerialName("display_name")
    val displayName: String
)

