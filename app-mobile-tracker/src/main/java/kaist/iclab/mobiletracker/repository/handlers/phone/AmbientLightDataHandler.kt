package kaist.iclab.mobiletracker.repository.handlers.phone

import kaist.iclab.mobiletracker.config.AppConfig
import kaist.iclab.mobiletracker.db.entity.phone.AmbientLightEntity
import kaist.iclab.mobiletracker.db.obx.SensorStore
import kaist.iclab.mobiletracker.repository.SensorRecord
import kaist.iclab.mobiletracker.repository.handlers.SensorDataHandler

/**
 * Handler for Ambient Light sensor data.
 */
class AmbientLightDataHandler(private val store: SensorStore<AmbientLightEntity>) : SensorDataHandler {
    override val sensorId = "AmbientLight"
    override val displayName = "Ambient Light"
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
                    "Value" to String.format("%.1f lux", entity.value)
                )
            )
        }

    override suspend fun deleteAll() = store.removeAll()
    override suspend fun deleteById(id: Long) { store.removeById(id) }
    override suspend fun getEventIdById(id: Long) = store.eventIdById(id)
    override val supabaseTableName = AppConfig.SupabaseTables.AMBIENT_LIGHT_SENSOR
}
