package kaist.iclab.mobiletracker.repository.handlers.phone

import kaist.iclab.mobiletracker.config.AppConfig
import kaist.iclab.mobiletracker.db.entity.phone.DataTrafficEntity
import kaist.iclab.mobiletracker.db.obx.SensorStore
import kaist.iclab.mobiletracker.repository.SensorRecord
import kaist.iclab.mobiletracker.repository.handlers.SensorDataHandler

/**
 * Handler for Data Traffic sensor data.
 */
class DataTrafficDataHandler(private val store: SensorStore<DataTrafficEntity>) : SensorDataHandler {
    override val sensorId = "DataTraffic"
    override val displayName = "Data Traffic"
    override val isWatchSensor = false

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
                id = entity.id.toLong(),
                timestamp = entity.timestamp,
                fields = mapOf(
                    "Total Rx" to "${entity.totalRx / 1024} KB",
                    "Total Tx" to "${entity.totalTx / 1024} KB"
                )
            )
        }

    override suspend fun deleteAll() = store.removeAll()
    override suspend fun deleteById(id: Long) { store.removeById(id) }
    override suspend fun getEventIdById(id: Long) = store.eventIdById(id)
    override val supabaseTableName = AppConfig.SupabaseTables.DATA_TRAFFIC_SENSOR
}
