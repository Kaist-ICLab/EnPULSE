package kaist.iclab.mobiletracker.repository.handlers.watch

import kaist.iclab.mobiletracker.db.dao.watch.WatchIMUDao
import kaist.iclab.mobiletracker.repository.SensorRecord
import kaist.iclab.mobiletracker.repository.handlers.SensorDataHandler

class WatchIMUDataHandler(private val dao: WatchIMUDao) : SensorDataHandler {
    override val sensorId = "WatchIMU"
    override val displayName = "IMU"
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
                    "Acc X" to entity.accX.toString(),
                    "Acc Y" to entity.accY.toString(),
                    "Acc Z" to entity.accZ.toString(),
                    "Gyro X" to entity.gyroX.toString(),
                    "Gyro Y" to entity.gyroY.toString(),
                    "Gyro Z" to entity.gyroZ.toString()
                )
            )
        }

    override suspend fun deleteAll() = dao.deleteAll()
    override suspend fun deleteById(id: Long) = dao.deleteById(id)
    override suspend fun getEventIdById(id: Long) = dao.getEventIdById(id)
    override val supabaseTableName = "sensor_imu"
}
