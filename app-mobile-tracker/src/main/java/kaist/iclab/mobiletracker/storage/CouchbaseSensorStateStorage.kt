package kaist.iclab.mobiletracker.storage

import kaist.iclab.tracker.sensor.core.SensorState
import kaist.iclab.tracker.storage.couchbase.CouchbaseDB
import kaist.iclab.tracker.storage.couchbase.CouchbaseStateStorage

// To save the PHONE sensor state to Couchbase
class CouchbaseSensorStateStorage(
    couchbase: CouchbaseDB,
    collectionName: String,
) : CouchbaseStateStorage<SensorState>(
    couchbase = couchbase,
    defaultVal = SensorState(SensorState.FLAG.UNAVAILABLE),
    serializer = SensorState.serializer(),
    collectionName = collectionName
)

