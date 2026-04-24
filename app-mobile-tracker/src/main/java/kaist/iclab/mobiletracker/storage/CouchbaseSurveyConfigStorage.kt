package kaist.iclab.mobiletracker.storage

import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.tracker.sensor.survey.config.SurveyConfigList
import kaist.iclab.tracker.storage.couchbase.CouchbaseDB
import kaist.iclab.tracker.storage.couchbase.CouchbaseStateStorage

/**
 * Persists the raw Supabase survey configurations to Couchbase
 */
class CouchbaseSurveyConfigStorage(
    couchbase: CouchbaseDB,
    collectionName: String = "SurveyConfigStorage"
) : CouchbaseStateStorage<SurveyConfigList>(
    couchbase = couchbase,
    defaultVal = SurveyConfigList(),
    clazz = SurveyConfigList::class.java,
    collectionName = collectionName
)
