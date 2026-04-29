package kaist.iclab.mobiletracker.db.mapper

import kaist.iclab.mobiletracker.data.sensors.watch.StressSensorData
import kaist.iclab.mobiletracker.db.entity.watch.WatchStressEntity
import java.time.Instant

object StressMapper {
    fun map(entity: WatchStressEntity, userUuid: String): StressSensorData {
        return StressSensorData(
            eventId = entity.eventId,
            uuid = userUuid,
            deviceType = entity.deviceType,
            received = Instant.ofEpochMilli(entity.received).toString(),
            timestamp = Instant.ofEpochMilli(entity.timestamp).toString(),
            windowStart = Instant.ofEpochMilli(entity.windowStartMs).toString(),
            probability = entity.probability,
            isHighStress = entity.isHighStress
        )
    }
}
