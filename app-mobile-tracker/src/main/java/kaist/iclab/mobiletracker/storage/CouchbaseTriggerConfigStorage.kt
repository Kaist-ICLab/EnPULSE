package kaist.iclab.mobiletracker.storage

import kaist.iclab.mobiletracker.data.trigger.CampaignTriggerList
import kaist.iclab.tracker.storage.couchbase.CouchbaseDB
import kaist.iclab.tracker.storage.couchbase.CouchbaseStateStorage

/**
 * Persists the raw Supabase campaign trigger configurations to Couchbase.
 */
class CouchbaseTriggerConfigStorage(
    couchbase: CouchbaseDB,
    collectionName: String = "TriggerConfigStorage"
) : CouchbaseStateStorage<CampaignTriggerList>(
    couchbase = couchbase,
    defaultVal = CampaignTriggerList(),
    clazz = CampaignTriggerList::class.java,
    collectionName = collectionName
)
