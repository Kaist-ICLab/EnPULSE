package kaist.iclab.tracker.trigger.model

import kotlinx.serialization.Serializable

/**
 * A fully parsed campaign trigger configuration.
 *
 * Combines the trigger metadata (id, name) with a parsed [ConditionNode] tree
 * and a list of [TriggerActionConfig] actions.
 *
 * This is the runtime representation that the [TriggerEngine] operates on.
 * Created by parsing the raw Supabase `campaign_trigger` entity.
 */
@Serializable
data class ParsedCampaignTrigger(
    val id: Int,
    val campaignId: Int,
    val name: String,
    val condition: ConditionNode,
    @Serializable(with = TriggerActionConfigListSerializer::class)
    val actions: List<TriggerActionConfig>
)
