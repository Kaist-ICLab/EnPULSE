package kaist.iclab.mobiletracker.storage

import kaist.iclab.mobiletracker.data.sensors.CampaignSensorList
import kaist.iclab.tracker.storage.couchbase.CouchbaseDB
import kaist.iclab.tracker.storage.couchbase.CouchbaseStateStorage

/**
 * Persists the raw Supabase campaign sensor configurations to Couchbase.
 */
class CampaignSensorConfigStorage(
    couchbase: CouchbaseDB,
    collectionName: String = "CampaignSensorStorage"
) : CouchbaseStateStorage<CampaignSensorList>(
    couchbase = couchbase,
    defaultVal = CampaignSensorList(),
    serializer = CampaignSensorList.serializer(),
    collectionName = collectionName
)
