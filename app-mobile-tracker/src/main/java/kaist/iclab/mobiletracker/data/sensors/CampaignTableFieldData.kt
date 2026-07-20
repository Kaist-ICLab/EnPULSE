package kaist.iclab.mobiletracker.data.sensors

import kotlinx.serialization.Serializable

/**
 * Supabase data class representing campaign table field data.
 *
 * @property id Unique identifier for the field entry (integer primary key).
 * @property campaignId Campaign identifier.
 * @property campaignTableId Campaign table identifier.
 * @property name Name of the field.
 * @property description Description of the field.
 * @property fieldType Type of the field (e.g., "text", "number", "date").
 * @property fieldRole Role of the field (e.g., "input", "output").
 */
@Serializable
data class CampaignTableFieldData(
    val id: Int? = null,
    val campaignId: Int,
    val campaignTableId: Int,
    val name: String,
    val description: String,
    val fieldType: String,
    val fieldRole: String
)

