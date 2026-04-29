package kaist.iclab.wearabletracker.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kaist.iclab.tracker.sensor.galaxywatch.IMUSensor
import kaist.iclab.wearabletracker.db.entity.IMUEntity
import kaist.iclab.wearabletracker.db.entity.CsvSerializable

@Dao
interface IMUDao : BaseDao<IMUSensor.Entity> {
    override suspend fun insert(sensorEntity: IMUSensor.Entity) {
        val entity = IMUEntity(
            received = sensorEntity.received,
            timestamp = sensorEntity.timestamp,
            accX = sensorEntity.accX,
            accY = sensorEntity.accY,
            accZ = sensorEntity.accZ,
            gyroX = sensorEntity.gyroX,
            gyroY = sensorEntity.gyroY,
            gyroZ = sensorEntity.gyroZ
        )
        insertUsingRoomEntity(listOf(entity))
    }

    override suspend fun insert(sensorEntities: List<IMUSensor.Entity>) {
        val entities = sensorEntities.map { sensorEntity ->
            IMUEntity(
                received = sensorEntity.received,
                timestamp = sensorEntity.timestamp,
                accX = sensorEntity.accX,
                accY = sensorEntity.accY,
                accZ = sensorEntity.accZ,
                gyroX = sensorEntity.gyroX,
                gyroY = sensorEntity.gyroY,
                gyroZ = sensorEntity.gyroZ
            )
        }
        insertUsingRoomEntity(entities)
    }

    @Insert
    suspend fun insertUsingRoomEntity(entities: List<IMUEntity>)

    @Query("SELECT * FROM IMUEntity ORDER BY timestamp ASC")
    suspend fun getAllIMUData(): List<IMUEntity>

    override suspend fun getAllForExport(): List<CsvSerializable> = getAllIMUData()

    @Query("SELECT * FROM IMUEntity WHERE timestamp > :since ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getIMUDataSince(since: Long, limit: Int): List<IMUEntity>

    override suspend fun getDataSince(timestamp: Long, limit: Int): List<CsvSerializable> =
        getIMUDataSince(timestamp, limit)

    @Query("DELETE FROM IMUEntity WHERE timestamp <= :until")
    suspend fun deleteIMUDataBefore(until: Long)

    override suspend fun deleteDataBefore(timestamp: Long) =
        deleteIMUDataBefore(timestamp)

    @Query("DELETE FROM IMUEntity")
    suspend fun deleteAllIMUData()

    override suspend fun deleteAll() {
        deleteAllIMUData()
    }

    @Query("SELECT COUNT(*) FROM IMUEntity")
    override suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM IMUEntity WHERE timestamp > :timestamp")
    override suspend fun getCountSince(timestamp: Long): Int
}
