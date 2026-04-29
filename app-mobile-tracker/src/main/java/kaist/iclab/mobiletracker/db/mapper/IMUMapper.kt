package kaist.iclab.mobiletracker.db.mapper

import kaist.iclab.mobiletracker.data.sensors.watch.IMUSensorData
import kaist.iclab.mobiletracker.db.entity.watch.WatchIMUEntity
import java.time.Instant

object IMUMapper {
    fun map(entity: WatchIMUEntity, userUuid: String): IMUSensorData {
        return IMUSensorData(
            eventId = entity.eventId,
            uuid = userUuid,
            deviceType = entity.deviceType,
            received = Instant.ofEpochMilli(entity.received).toString(),
            timestamp = Instant.ofEpochMilli(entity.timestamp).toString(),
            accX = entity.accX,
            accY = entity.accY,
            accZ = entity.accZ,
            gyroX = entity.gyroX,
            gyroY = entity.gyroY,
            gyroZ = entity.gyroZ
        )
    }
}
