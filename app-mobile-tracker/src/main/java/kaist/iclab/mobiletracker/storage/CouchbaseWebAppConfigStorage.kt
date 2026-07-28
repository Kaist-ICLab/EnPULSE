package kaist.iclab.mobiletracker.storage

import kaist.iclab.mobiletracker.data.campaign.WebAppConfigList
import kaist.iclab.tracker.storage.couchbase.CouchbaseDB
import kaist.iclab.tracker.storage.couchbase.CouchbaseStateStorage

/**
 * Persists the raw Supabase webapp configurations to Couchbase
 */
class CouchbaseWebAppConfigStorage(
    couchbase: CouchbaseDB,
    collectionName: String = "WebAppConfigStorage"
) : CouchbaseStateStorage<WebAppConfigList>(
    couchbase = couchbase,
    defaultVal = WebAppConfigList(),
    serializer = WebAppConfigList.serializer(),
    collectionName = collectionName
)
