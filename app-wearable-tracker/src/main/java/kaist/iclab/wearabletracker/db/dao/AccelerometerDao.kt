package kaist.iclab.wearabletracker.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kaist.iclab.tracker.sensor.galaxywatch.AccelerometerSensor
import kaist.iclab.wearabletracker.db.entity.AccelerometerEntity
import kaist.iclab.wearabletracker.db.entity.CsvSerializable

@Dao
interface AccelerometerDao : BaseDao<AccelerometerSensor.Entity> {
    override suspend fun insert(sensorEntity: AccelerometerSensor.Entity) {
        val entity = sensorEntity.dataPoint.map {
            AccelerometerEntity(
                received = it.received,
                timestamp = it.timestamp,
                x = it.x,
                y = it.y,
                z = it.z
            )
        }
        insertUsingRoomEntity(entity)
    }

    @Insert
    suspend fun insertUsingRoomEntity(accelerometerEntity: List<AccelerometerEntity>)

    @Query("SELECT * FROM AccelerometerEntity ORDER BY timestamp ASC")
    suspend fun getAllAccelerometerData(): List<AccelerometerEntity>

    override suspend fun getAllForExport(): List<CsvSerializable> = getAllAccelerometerData()

    @Query("SELECT * FROM AccelerometerEntity WHERE timestamp > :since ORDER BY timestamp ASC")
    suspend fun getAccelerometerDataSince(since: Long): List<AccelerometerEntity>

    override suspend fun getDataSince(timestamp: Long): List<CsvSerializable> =
        getAccelerometerDataSince(timestamp)

    @Query("DELETE FROM AccelerometerEntity WHERE timestamp <= :until")
    suspend fun deleteAccelerometerDataBefore(until: Long)

    override suspend fun deleteDataBefore(timestamp: Long) =
        deleteAccelerometerDataBefore(timestamp)

    @Query("DELETE FROM AccelerometerEntity")
    suspend fun deleteAllAccelerometerData()

    override suspend fun deleteAll() {
        deleteAllAccelerometerData()
    }
}