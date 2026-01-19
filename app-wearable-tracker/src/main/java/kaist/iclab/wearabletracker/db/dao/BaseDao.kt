package kaist.iclab.wearabletracker.db.dao

import kaist.iclab.tracker.sensor.core.SensorEntity
import kaist.iclab.wearabletracker.db.entity.CsvSerializable

interface BaseDao<T : SensorEntity> {
    suspend fun insert(sensorEntity: T)
    suspend fun deleteAll()

    /**
     * Get all data for CSV export.
     */
    suspend fun getAllForExport(): List<CsvSerializable>

    /**
     * Get data since the given timestamp for incremental sync.
     * @param timestamp Only return records with timestamp > this value
     */
    suspend fun getDataSince(timestamp: Long): List<CsvSerializable>

    /**
     * Delete data before/up to the given timestamp after successful sync.
     * @param timestamp Delete records with timestamp <= this value
     */
    suspend fun deleteDataBefore(timestamp: Long)
}