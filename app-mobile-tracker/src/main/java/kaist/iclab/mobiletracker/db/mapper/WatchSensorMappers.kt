package kaist.iclab.mobiletracker.db.mapper

import java.time.Instant
import kaist.iclab.mobiletracker.data.DeviceType
import kaist.iclab.mobiletracker.data.sensors.watch.AccelerometerSensorData
import kaist.iclab.mobiletracker.data.sensors.watch.EDASensorData
import kaist.iclab.mobiletracker.data.sensors.watch.HeartRateSensorData
import kaist.iclab.mobiletracker.data.sensors.common.LocationSensorData
import kaist.iclab.mobiletracker.data.sensors.watch.PPGSensorData
import kaist.iclab.mobiletracker.data.sensors.watch.SkinTemperatureSensorData
import kaist.iclab.mobiletracker.db.entity.watch.WatchAccelerometerEntity
import kaist.iclab.mobiletracker.db.entity.watch.WatchEDAEntity
import kaist.iclab.mobiletracker.db.entity.watch.WatchHeartRateEntity
import kaist.iclab.mobiletracker.db.entity.common.LocationEntity
import kaist.iclab.mobiletracker.db.entity.watch.WatchPPGEntity
import kaist.iclab.mobiletracker.db.entity.watch.WatchSkinTemperatureEntity

object HeartRateMapper : EntityToSupabaseMapper<WatchHeartRateEntity, HeartRateSensorData> {
    override fun map(entity: WatchHeartRateEntity, userUuid: String?): HeartRateSensorData {
        return HeartRateSensorData(
            eventId = entity.eventId,
            uuid = userUuid,
            deviceType = DeviceType.WATCH.value,
            timestamp = Instant.ofEpochMilli(entity.timestamp).toString(),
            hr = entity.hr,
            hrStatus = entity.hrStatus,
            ibi = entity.ibi,
            ibiStatus = entity.ibiStatus,
            received = Instant.ofEpochMilli(entity.received).toString()
        )
    }
}

object AccelerometerMapper : EntityToSupabaseMapper<WatchAccelerometerEntity, AccelerometerSensorData> {
    override fun map(entity: WatchAccelerometerEntity, userUuid: String?): AccelerometerSensorData {
        return AccelerometerSensorData(
            eventId = entity.eventId,
            uuid = userUuid,
            deviceType = DeviceType.WATCH.value,
            timestamp = Instant.ofEpochMilli(entity.timestamp).toString(),
            x = entity.x,
            y = entity.y,
            z = entity.z,
            received = Instant.ofEpochMilli(entity.received).toString()
        )
    }
}

object EDAMapper : EntityToSupabaseMapper<WatchEDAEntity, EDASensorData> {
    override fun map(entity: WatchEDAEntity, userUuid: String?): EDASensorData {
        return EDASensorData(
            eventId = entity.eventId,
            uuid = userUuid,
            deviceType = DeviceType.WATCH.value,
            timestamp = Instant.ofEpochMilli(entity.timestamp).toString(),
            skinConductance = entity.skinConductance,
            status = entity.status,
            received = Instant.ofEpochMilli(entity.received).toString()
        )
    }
}

object PPGMapper : EntityToSupabaseMapper<WatchPPGEntity, PPGSensorData> {
    override fun map(entity: WatchPPGEntity, userUuid: String?): PPGSensorData {
        return PPGSensorData(
            eventId = entity.eventId,
            uuid = userUuid,
            deviceType = DeviceType.WATCH.value,
            timestamp = Instant.ofEpochMilli(entity.timestamp).toString(),
            green = entity.green,
            greenStatus = entity.greenStatus,
            red = entity.red,
            redStatus = entity.redStatus,
            ir = entity.ir,
            irStatus = entity.irStatus,
            received = Instant.ofEpochMilli(entity.received).toString()
        )
    }
}

object SkinTemperatureMapper : EntityToSupabaseMapper<WatchSkinTemperatureEntity, SkinTemperatureSensorData> {
    override fun map(entity: WatchSkinTemperatureEntity, userUuid: String?): SkinTemperatureSensorData {
        return SkinTemperatureSensorData(
            eventId = entity.eventId,
            uuid = userUuid,
            deviceType = DeviceType.WATCH.value,
            timestamp = Instant.ofEpochMilli(entity.timestamp).toString(),
            ambientTemp = entity.ambientTemp,
            objectTemp = entity.objectTemp,
            status = entity.status,
            received = Instant.ofEpochMilli(entity.received).toString()
        )
    }
}

object LocationMapper : EntityToSupabaseMapper<LocationEntity, LocationSensorData> {
    override fun map(entity: LocationEntity, userUuid: String?): LocationSensorData {
        return LocationSensorData(
            eventId = entity.eventId,
            uuid = userUuid,
            deviceType = entity.deviceType,
            timestamp = Instant.ofEpochMilli(entity.timestamp).toString(),
            latitude = entity.latitude,
            longitude = entity.longitude,
            altitude = entity.altitude,
            speed = entity.speed,
            accuracy = entity.accuracy,
            received = Instant.ofEpochMilli(entity.received).toString()
        )
    }
}
