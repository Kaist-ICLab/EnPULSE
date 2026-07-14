package kaist.iclab.mobiletracker.repository.handlers.watch

import kaist.iclab.mobiletracker.db.dao.watch.WatchGestureDao
import kaist.iclab.mobiletracker.repository.SensorRecord
import kaist.iclab.mobiletracker.repository.handlers.SensorDataHandler

class WatchGestureDataHandler(private val dao: WatchGestureDao) : SensorDataHandler {
    override val sensorId = "WatchGesture"
    override val displayName = "Gesture"
    override val isWatchSensor = true

    override suspend fun getRecordCount() = dao.getRecordCount()
    override suspend fun getLatestTimestamp() = dao.getLatestTimestamp()
    override suspend fun getRecordCountAfterTimestamp(timestamp: Long) =
        dao.getRecordCountAfterTimestamp(timestamp)

    override suspend fun getRecordsPaginated(
        afterTimestamp: Long,
        isAscending: Boolean,
        limit: Int,
        offset: Int
    ): List<SensorRecord> = dao.getRecordsPaginated(afterTimestamp, isAscending, limit, offset)
        .map { entity ->
            SensorRecord(
                id = entity.id,
                timestamp = entity.timestamp,
                fields = mapOf(
                    "Class Index" to entity.classIndex.toString(),
                    "Score" to entity.score.toString(),
                    "Probabilities" to entity.probabilities.joinToString(", ")
                )
            )
        }

    override suspend fun deleteAll() = dao.deleteAll()
    override suspend fun deleteById(id: Long) = dao.deleteById(id)
    override suspend fun getEventIdById(id: Long) = dao.getEventIdById(id)
    override val supabaseTableName = "sensor_gesture"
}
