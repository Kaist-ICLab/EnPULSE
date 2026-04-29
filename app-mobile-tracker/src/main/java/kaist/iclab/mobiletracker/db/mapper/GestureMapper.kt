package kaist.iclab.mobiletracker.db.mapper

import kaist.iclab.mobiletracker.data.sensors.watch.GestureSupabaseData
import kaist.iclab.mobiletracker.db.entity.watch.WatchGestureEntity
import java.time.Instant

object GestureMapper {
    fun map(entity: WatchGestureEntity, userUuid: String): GestureSupabaseData {
        return GestureSupabaseData(
            eventId = entity.eventId,
            uuid = userUuid,
            deviceType = entity.deviceType,
            received = Instant.ofEpochMilli(entity.received).toString(),
            timestamp = Instant.ofEpochMilli(entity.timestamp).toString(),
            classIndex = entity.classIndex,
            score = entity.score,
            probabilities = entity.probabilities
        )
    }
}
