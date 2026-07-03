package kaist.iclab.mobiletracker.repository.handlers

import kaist.iclab.mobiletracker.db.obx.SensorStore
import kaist.iclab.mobiletracker.repository.SensorRecord

/**
 * Generic read handler backed by a [SensorStore]. Replaces the per-sensor `*DataHandler` classes:
 * everything except the metadata and the entity→[SensorRecord] display projection was identical
 * delegation, so a sensor now only needs to supply those via [toRecord].
 */
class GenericSensorDataHandler<T : Any>(
    override val sensorId: String,
    override val displayName: String,
    override val isWatchSensor: Boolean,
    override val supabaseTableName: String,
    private val store: SensorStore<T>,
    private val toRecord: (T) -> SensorRecord
) : SensorDataHandler {
    override suspend fun getRecordCount() = store.count().toInt()
    override suspend fun getLatestTimestamp() = store.latestTimestamp()
    override suspend fun getRecordCountAfterTimestamp(timestamp: Long) =
        store.countAfter(timestamp).toInt()

    override suspend fun getRecordsPaginated(
        afterTimestamp: Long,
        isAscending: Boolean,
        limit: Int,
        offset: Int
    ): List<SensorRecord> =
        store.recordsAfter(afterTimestamp, isAscending, limit, offset).map(toRecord)

    override suspend fun deleteAll() = store.removeAll()
    override suspend fun deleteById(id: Long) { store.removeById(id) }
    override suspend fun getEventIdById(id: Long) = store.eventIdById(id)
}
