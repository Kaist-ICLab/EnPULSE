package kaist.iclab.wearabletracker.db.obx

import io.objectbox.BoxStore
import kaist.iclab.tracker.sensor.common.LocationSensor
import kaist.iclab.tracker.sensor.galaxywatch.GestureSensor
import kaist.iclab.tracker.sensor.galaxywatch.IMUSensor
import kaist.iclab.wearabletracker.db.entity.GestureEntity
import kaist.iclab.wearabletracker.db.entity.IMUEntity
import kaist.iclab.wearabletracker.db.entity.LocationEntity

/**
 * Stores for the sensors every build has. The Samsung Health sensors' stores are in the samsung
 * flavor's SamsungWatchSensorStores; their ObjectBox entities stay here so the model is the same
 * in both flavors.
 */
class WatchSensorStores(boxStore: BoxStore) {
    val location = WatchSensorStore(boxStore, LocationEntity::class.java) { e ->
        e as LocationSensor.Entity
        listOf(
            LocationEntity(
                e.received,
                e.timestamp,
                e.latitude,
                e.longitude,
                e.altitude,
                e.speed,
                e.accuracy
            )
        )
    }

    val watchIMU = WatchSensorStore(boxStore, IMUEntity::class.java) { e ->
        e as IMUSensor.Entity
        listOf(
            IMUEntity(
                e.received,
                e.timestamp,
                e.accX,
                e.accY,
                e.accZ,
                e.gyroX,
                e.gyroY,
                e.gyroZ
            )
        )
    }

    val watchGesture = WatchSensorStore(boxStore, GestureEntity::class.java) { e ->
        e as GestureSensor.Entity
        listOf(
            GestureEntity(
                e.received,
                e.timestamp,
                e.classIndex,
                e.score,
                e.probabilities.toIntArray()
            )
        )
    }
}
