package kaist.iclab.wearabletracker.db.obx

import io.objectbox.BoxStore
import kaist.iclab.tracker.sensor.common.LocationSensor
import kaist.iclab.tracker.sensor.galaxywatch.AccelerometerSensor
import kaist.iclab.tracker.sensor.galaxywatch.ECGSensor
import kaist.iclab.tracker.sensor.galaxywatch.EDASensor
import kaist.iclab.tracker.sensor.galaxywatch.HeartRateSensor
import kaist.iclab.tracker.sensor.galaxywatch.PPGSensor
import kaist.iclab.tracker.sensor.galaxywatch.SkinTemperatureSensor
import kaist.iclab.wearabletracker.db.entity.AccelerometerEntity
import kaist.iclab.wearabletracker.db.entity.ECGEntity
import kaist.iclab.wearabletracker.db.entity.EDAEntity
import kaist.iclab.wearabletracker.db.entity.HeartRateEntity
import kaist.iclab.wearabletracker.db.entity.LocationEntity
import kaist.iclab.wearabletracker.db.entity.PPGEntity
import kaist.iclab.wearabletracker.db.entity.SkinTemperatureEntity

class WatchSensorStores(boxStore: BoxStore) {
    val accelerometer = WatchSensorStore(boxStore, AccelerometerEntity::class.java) { e ->
        e as AccelerometerSensor.Entity
        e.dataPoint.map { AccelerometerEntity(it.received, it.timestamp, it.x, it.y, it.z) }
    }

    val ppg = WatchSensorStore(boxStore, PPGEntity::class.java) { e ->
        e as PPGSensor.Entity
        e.dataPoint.map {
            PPGEntity(it.received, it.timestamp, it.green, it.red, it.ir, it.greenStatus, it.redStatus, it.irStatus)
        }
    }

    val heartRate = WatchSensorStore(boxStore, HeartRateEntity::class.java) { e ->
        e as HeartRateSensor.Entity
        e.dataPoint.map {
            HeartRateEntity(it.received, it.timestamp, it.hr, it.hrStatus, it.ibi.toIntArray(), it.ibiStatus.toIntArray())
        }
    }

    val skinTemperature = WatchSensorStore(boxStore, SkinTemperatureEntity::class.java) { e ->
        e as SkinTemperatureSensor.Entity
        e.dataPoint.map {
            SkinTemperatureEntity(it.received, it.timestamp, it.objectTemperature, it.ambientTemperature, it.status)
        }
    }

    val eda = WatchSensorStore(boxStore, EDAEntity::class.java) { e ->
        e as EDASensor.Entity
        e.dataPoint.map { EDAEntity(it.received, it.timestamp, it.skinConductance, it.status) }
    }

    val location = WatchSensorStore(boxStore, LocationEntity::class.java) { e ->
        e as LocationSensor.Entity
        listOf(LocationEntity(e.received, e.timestamp, e.latitude, e.longitude, e.altitude, e.speed, e.accuracy))
    }

    val ecg = WatchSensorStore(boxStore, ECGEntity::class.java) { e ->
        e as ECGSensor.Entity
        e.dataPoint.map {
            ECGEntity(it.received, it.timestamp, it.ecgMv, it.leadOff, it.sequence, it.ppgGreen, it.maxThresholdMv, it.minThresholdMv)
        }
    }
}
