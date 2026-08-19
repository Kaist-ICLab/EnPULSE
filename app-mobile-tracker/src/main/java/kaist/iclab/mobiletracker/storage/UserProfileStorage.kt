package kaist.iclab.mobiletracker.storage

import kaist.iclab.mobiletracker.data.campaign.ProfileData
import kaist.iclab.tracker.storage.couchbase.CouchbaseDB
import kaist.iclab.tracker.storage.couchbase.CouchbaseStateStorage

/**
 * Persists the user profile (including campaign ID) locally to Couchbase
 * to allow offline startup and configuration stability.
 */
class UserProfileStorage(
    couchbase: CouchbaseDB,
    collectionName: String = "UserProfileStorage"
) : CouchbaseStateStorage<ProfileData>(
    couchbase = couchbase,
    defaultVal = ProfileData(),
    serializer = ProfileData.serializer(),
    collectionName = collectionName
)
