package kaist.iclab.mobiletracker.data.sensors.phone

import kotlinx.serialization.Serializable

/**
 * Wrapper class for a list of campaign sensors to enable serialization with CouchbaseStateStorage.
 */
@Serializable
data class CampaignSensorList(
    val sensors: List<CampaignTableData> = emptyList()
)
