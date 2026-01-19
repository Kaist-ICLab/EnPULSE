package kaist.iclab.mobiletracker.repository.handlers.watch

import kaist.iclab.mobiletracker.db.dao.watch.WatchSkinTemperatureDao
import kaist.iclab.mobiletracker.config.AppConfig
import kaist.iclab.mobiletracker.repository.SensorRecord
import kaist.iclab.mobiletracker.repository.handlers.SensorDataHandler

/**
 * Handler for Watch Skin Temperature sensor data.
 */
class WatchSkinTemperatureDataHandler(private val dao: WatchSkinTemperatureDao) : SensorDataHandler {
    override val sensorId = "WatchSkinTemperature"
    override val displayName = "Skin Temperature"
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
                    "Skin Temp" to String.format("%.1f°C", entity.objectTemp)
                )
            )
        }

    override suspend fun deleteAll() = dao.deleteAll()
    override suspend fun deleteById(id: Long) = dao.deleteById(id)
    override suspend fun getEventIdById(id: Long) = dao.getEventIdById(id)
    override val supabaseTableName = AppConfig.SupabaseTables.SKIN_TEMPERATURE_SENSOR
}
