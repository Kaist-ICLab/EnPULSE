package kaist.iclab.mobiletracker.repository.handlers.watch

import kaist.iclab.mobiletracker.config.AppConfig
import kaist.iclab.mobiletracker.db.entity.watch.WatchSkinTemperatureEntity
import kaist.iclab.mobiletracker.db.obx.SensorStore
import kaist.iclab.mobiletracker.repository.SensorRecord
import kaist.iclab.mobiletracker.repository.handlers.SensorDataHandler

/**
 * Handler for Watch Skin Temperature sensor data.
 */
class WatchSkinTemperatureDataHandler(private val store: SensorStore<WatchSkinTemperatureEntity>) :
    SensorDataHandler {
    override val sensorId = "WatchSkinTemperature"
    override val displayName = "Skin Temperature"
    override val isWatchSensor = true

    override suspend fun getRecordCount() = store.count().toInt()
    override suspend fun getLatestTimestamp() = store.latestTimestamp()
    override suspend fun getRecordCountAfterTimestamp(timestamp: Long) =
        store.countAfter(timestamp).toInt()

    override suspend fun getRecordsPaginated(
        afterTimestamp: Long,
        isAscending: Boolean,
        limit: Int,
        offset: Int
    ): List<SensorRecord> = store.recordsAfter(afterTimestamp, isAscending, limit, offset)
        .map { entity ->
            SensorRecord(
                id = entity.id,
                timestamp = entity.timestamp,
                fields = mapOf(
                    "Skin Temp" to String.format("%.1f°C", entity.objectTemp)
                )
            )
        }

    override suspend fun deleteAll() = store.removeAll()
    override suspend fun deleteById(id: Long) { store.removeById(id) }
    override suspend fun getEventIdById(id: Long) = store.eventIdById(id)
    override val supabaseTableName = AppConfig.SupabaseTables.SKIN_TEMPERATURE_SENSOR
}
