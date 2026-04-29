package kaist.iclab.wearabletracker.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kaist.iclab.tracker.sensor.galaxywatch.GestureSensor
import kaist.iclab.wearabletracker.db.entity.GestureEntity
import kaist.iclab.wearabletracker.db.entity.CsvSerializable

@Dao
interface GestureDao : BaseDao<GestureSensor.Entity> {
    override suspend fun insert(sensorEntity: GestureSensor.Entity) {
        val entity = GestureEntity(
            received = sensorEntity.received,
            timestamp = sensorEntity.timestamp,
            classIndex = sensorEntity.classIndex,
            score = sensorEntity.score,
            probabilities = sensorEntity.probabilities
        )
        insertUsingRoomEntity(listOf(entity))
    }

    override suspend fun insert(sensorEntities: List<GestureSensor.Entity>) {
        val entities = sensorEntities.map { sensorEntity ->
            GestureEntity(
                received = sensorEntity.received,
                timestamp = sensorEntity.timestamp,
                classIndex = sensorEntity.classIndex,
                score = sensorEntity.score,
                probabilities = sensorEntity.probabilities
            )
        }
        insertUsingRoomEntity(entities)
    }

    @Insert
    suspend fun insertUsingRoomEntity(entities: List<GestureEntity>)

    @Query("SELECT * FROM GestureEntity ORDER BY timestamp ASC")
    suspend fun getAllGestureData(): List<GestureEntity>

    override suspend fun getAllForExport(): List<CsvSerializable> = getAllGestureData()

    @Query("SELECT * FROM GestureEntity WHERE timestamp > :since ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getGestureDataSince(since: Long, limit: Int): List<GestureEntity>

    override suspend fun getDataSince(timestamp: Long, limit: Int): List<CsvSerializable> =
        getGestureDataSince(timestamp, limit)

    @Query("DELETE FROM GestureEntity WHERE timestamp <= :until")
    suspend fun deleteGestureDataBefore(until: Long)

    override suspend fun deleteDataBefore(timestamp: Long) =
        deleteGestureDataBefore(timestamp)

    @Query("DELETE FROM GestureEntity")
    suspend fun deleteAllGestureData()

    override suspend fun deleteAll() {
        deleteAllGestureData()
    }

    @Query("SELECT COUNT(*) FROM GestureEntity")
    override suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM GestureEntity WHERE timestamp > :timestamp")
    override suspend fun getCountSince(timestamp: Long): Int
}
