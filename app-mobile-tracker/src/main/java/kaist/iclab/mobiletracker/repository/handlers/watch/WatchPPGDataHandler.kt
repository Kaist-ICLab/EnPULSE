package kaist.iclab.mobiletracker.repository.handlers.watch

import kaist.iclab.mobiletracker.config.AppConfig
import kaist.iclab.mobiletracker.db.entity.watch.WatchPPGEntity
import kaist.iclab.mobiletracker.db.obx.SensorStore
import kaist.iclab.mobiletracker.repository.SensorRecord
import kaist.iclab.mobiletracker.repository.handlers.SensorDataHandler

/**
 * Handler for Watch PPG sensor data.
 */
class WatchPPGDataHandler(private val store: SensorStore<WatchPPGEntity>) : SensorDataHandler {
    override val sensorId = "WatchPPG"
    override val displayName = "PPG"
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
                    "Green" to entity.green.toString(),
                    "IR" to entity.ir.toString()
                )
            )
        }

    override suspend fun deleteAll() = store.removeAll()
    override suspend fun deleteById(id: Long) { store.removeById(id) }
    override suspend fun getEventIdById(id: Long) = store.eventIdById(id)
    override val supabaseTableName = AppConfig.SupabaseTables.PPG_SENSOR
}
