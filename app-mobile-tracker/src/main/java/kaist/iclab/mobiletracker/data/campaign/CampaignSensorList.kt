package kaist.iclab.mobiletracker.data.campaign

import kotlinx.serialization.Serializable

/**
 * Wrapper class for a list of campaign sensors to enable serialization with CouchbaseStateStorage.
 */
@Serializable
data class CampaignSensorList(
    val sensors: List<CampaignTableData> = emptyList()
)