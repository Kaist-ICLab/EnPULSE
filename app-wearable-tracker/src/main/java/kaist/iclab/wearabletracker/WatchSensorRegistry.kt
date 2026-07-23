package kaist.iclab.wearabletracker

import kaist.iclab.tracker.sensor.core.Sensor
import kaist.iclab.tracker.sensor.core.SensorState
import kaist.iclab.tracker.storage.couchbase.CouchbaseStateStorage
import kaist.iclab.wearabletracker.data.CampaignSensorConfig
import kaist.iclab.wearabletracker.db.obx.WatchSensorStore
import kotlinx.serialization.serializer
import org.koin.core.scope.Scope
import kotlin.reflect.KClass

data class WatchSensorDescriptor(
    val sensor: Sensor<*, *>,
    val store: WatchSensorStore<*>? = null
)

inline fun <reified C : Any> Scope.sensorConfig(sensorClass: KClass<*>, default: C): CouchbaseStateStorage<C> =
    CouchbaseStateStorage(
        couchbase = get(),
        defaultVal = default,
        serializer = serializer<C>(),
        collectionName = (sensorClass.simpleName ?: "") + "config"
    )

fun Scope.sensorState(sensorClass: KClass<*>): CouchbaseStateStorage<SensorState> =
    CouchbaseStateStorage(
        couchbase = get(),
        defaultVal = SensorState(SensorState.FLAG.UNAVAILABLE),
        serializer = SensorState.serializer(),
        collectionName = sensorClass.simpleName ?: ""
    )

fun Scope.campaignSensorConfigStorage(): CouchbaseStateStorage<CampaignSensorConfig> =
    CouchbaseStateStorage(
        couchbase = get(),
        defaultVal = CampaignSensorConfig(),
        serializer = CampaignSensorConfig.serializer(),
        collectionName = "campaignSensorConfig"
    )
