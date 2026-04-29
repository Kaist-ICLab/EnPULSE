package kaist.iclab.mobiletracker.data.trigger

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Supabase entity for the 'campaign_trigger' table
 */
@Serializable
data class CampaignTriggerEntity(
    val id: Int,
    @SerialName("campaign_id") val campaignId: Int,
    val name: String,
    val condition: JsonElement, // Polymorphic boolean logic tree
    val action: JsonElement    // List of actions
)

/**
 * Wrapper for a list of trigger configurations to be persisted in local storage.
 */
@Serializable
data class CampaignTriggerList(
    val triggers: List<CampaignTriggerEntity> = emptyList()
)
