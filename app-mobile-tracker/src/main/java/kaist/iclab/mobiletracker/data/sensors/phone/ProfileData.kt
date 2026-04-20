package kaist.iclab.mobiletracker.data.sensors.phone

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Supabase data class representing profile data from the phone device.
 *
 * @property uuid Unique identifier for the profile entry. Auto-generated when inserting into Supabase.
 * @property campaignId Campaign identifier associated with the profile.
 * @property email Email address of the profile.
 */
@Serializable
data class ProfileData(
    val uuid: String,
    @SerialName("campaign_id")
    val campaignId: Int? = null,
    val email: String
)

