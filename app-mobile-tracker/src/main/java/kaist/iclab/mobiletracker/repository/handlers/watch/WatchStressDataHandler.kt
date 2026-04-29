package kaist.iclab.mobiletracker.repository.handlers.watch

import kaist.iclab.mobiletracker.db.dao.watch.WatchStressDao
import kaist.iclab.mobiletracker.repository.SensorRecord
import kaist.iclab.mobiletracker.repository.handlers.SensorDataHandler

class WatchStressDataHandler(private val dao: WatchStressDao) : SensorDataHandler {
    override val sensorId = "WatchStress"
    override val displayName = "Stress"
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
                    "Probability" to String.format("%.2f", entity.probability),
                    "High Stress" to entity.isHighStress.toString(),
                    "Window Start" to entity.windowStartMs.toString()
                )
            )
        }

    override suspend fun deleteAll() = dao.deleteAll()
    override suspend fun deleteById(id: Long) = dao.deleteById(id)
    override suspend fun getEventIdById(id: Long) = dao.getEventIdById(id)
    override val supabaseTableName = "sensor_stress"
}
