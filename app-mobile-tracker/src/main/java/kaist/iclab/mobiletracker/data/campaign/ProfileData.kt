package kaist.iclab.mobiletracker.data.campaign

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Supabase data class representing profile data from the phone device.
 *
 * @property uuid Unique identifier for the profile entry. Auto-generated when inserting into Supabase.
 * @property campaignId Campaign identifier associated with the profile.
 * @property email Email address of the profile.
 * @property resetPassword Whether the user has replaced their initial password. False means the
 *   app must send them to change it before anything else. Null means not known yet (a profile
 *   cached before this column existed) and is treated as no reset needed. Never sent while null,
 *   so a newly created profile row gets the column's database default.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ProfileData(
    val uuid: String = "",
    @SerialName("campaign_id")
    val campaignId: Int? = null,
    val email: String = "",
    @SerialName("reset_password")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val resetPassword: Boolean? = null
)