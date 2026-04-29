package kaist.iclab.wearabletracker.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kaist.iclab.tracker.sensor.galaxywatch.StressSensor
import kaist.iclab.wearabletracker.db.entity.StressEntity
import kaist.iclab.wearabletracker.db.entity.CsvSerializable

@Dao
interface StressDao : BaseDao<StressSensor.Entity> {
    override suspend fun insert(sensorEntity: StressSensor.Entity) {
        val entity = StressEntity(
            received = sensorEntity.received,
            timestamp = sensorEntity.timestamp,
            windowStartMs = sensorEntity.windowStartMs,
            probability = sensorEntity.probability,
            isHighStress = sensorEntity.isHighStress
        )
        insertUsingRoomEntity(listOf(entity))
    }

    override suspend fun insert(sensorEntities: List<StressSensor.Entity>) {
        val entities = sensorEntities.map { sensorEntity ->
            StressEntity(
                received = sensorEntity.received,
                timestamp = sensorEntity.timestamp,
                windowStartMs = sensorEntity.windowStartMs,
                probability = sensorEntity.probability,
                isHighStress = sensorEntity.isHighStress
            )
        }
        insertUsingRoomEntity(entities)
    }

    @Insert
    suspend fun insertUsingRoomEntity(entities: List<StressEntity>)

    @Query("SELECT * FROM StressEntity ORDER BY timestamp ASC")
    suspend fun getAllStressData(): List<StressEntity>

    override suspend fun getAllForExport(): List<CsvSerializable> = getAllStressData()

    @Query("SELECT * FROM StressEntity WHERE timestamp > :since ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getStressDataSince(since: Long, limit: Int): List<StressEntity>

    override suspend fun getDataSince(timestamp: Long, limit: Int): List<CsvSerializable> =
        getStressDataSince(timestamp, limit)

    @Query("DELETE FROM StressEntity WHERE timestamp <= :until")
    suspend fun deleteStressDataBefore(until: Long)

    override suspend fun deleteDataBefore(timestamp: Long) =
        deleteStressDataBefore(timestamp)

    @Query("DELETE FROM StressEntity")
    suspend fun deleteAllStressData()

    override suspend fun deleteAll() {
        deleteAllStressData()
    }

    @Query("SELECT COUNT(*) FROM StressEntity")
    override suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM StressEntity WHERE timestamp > :timestamp")
    override suspend fun getCountSince(timestamp: Long): Int
}
