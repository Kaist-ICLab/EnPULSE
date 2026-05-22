package kaist.iclab.mobiletracker.db.mapper

import kaist.iclab.mobiletracker.data.sensors.phone.ActivityRecognitionSensorData
import kaist.iclab.mobiletracker.db.entity.phone.ActivityRecognitionEntity

object ActivityRecognitionMapper {
    fun map(entity: ActivityRecognitionEntity, userUuid: String): ActivityRecognitionSensorData {
        return ActivityRecognitionSensorData(
            eventId = entity.eventId,
            uuid = userUuid,
            deviceType = 0, // Phone
            timestamp = entity.timestamp.toString(),
            received = entity.received.toString(),
            elapsedRealtimeMillis = entity.elapsedRealtimeMillis,
            activityType = entity.activityType,
            score = entity.score,
            probabilities = entity.probabilities
        )
    }
}
