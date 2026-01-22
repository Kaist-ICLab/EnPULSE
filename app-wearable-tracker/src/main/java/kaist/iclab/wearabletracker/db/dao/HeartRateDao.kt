package kaist.iclab.wearabletracker.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kaist.iclab.tracker.sensor.galaxywatch.HeartRateSensor
import kaist.iclab.wearabletracker.db.entity.CsvSerializable
import kaist.iclab.wearabletracker.db.entity.HeartRateEntity

@Dao
interface HeartRateDao : BaseDao<HeartRateSensor.Entity> {
    override suspend fun insert(sensorEntity: HeartRateSensor.Entity) {
        val entity = sensorEntity.dataPoint.map {
            HeartRateEntity(
                received = it.received,
                timestamp = it.timestamp,
                hr = it.hr,
                hrStatus = it.hrStatus,
                ibi = it.ibi,
                ibiStatus = it.ibiStatus
            )
        }
        insertUsingRoomEntity(entity)
    }

    override suspend fun insert(sensorEntities: List<HeartRateSensor.Entity>) {
        val entities = sensorEntities.flatMap { sensorEntity ->
            sensorEntity.dataPoint.map {
                HeartRateEntity(
                    received = it.received,
                    timestamp = it.timestamp,
                    hr = it.hr,
                    hrStatus = it.hrStatus,
                    ibi = it.ibi,
                    ibiStatus = it.ibiStatus
                )
            }
        }
        insertUsingRoomEntity(entities)
    }

    @Insert
    suspend fun insertUsingRoomEntity(heartRateEntity: List<HeartRateEntity>)

    @Query("SELECT * FROM HeartRateEntity ORDER BY timestamp ASC")
    suspend fun getAllHeartRateData(): List<HeartRateEntity>

    override suspend fun getAllForExport(): List<CsvSerializable> = getAllHeartRateData()

    @Query("SELECT * FROM HeartRateEntity WHERE timestamp > :since ORDER BY timestamp ASC")
    suspend fun getHeartRateDataSince(since: Long): List<HeartRateEntity>

    override suspend fun getDataSince(timestamp: Long): List<CsvSerializable> =
        getHeartRateDataSince(timestamp)

    @Query("DELETE FROM HeartRateEntity WHERE timestamp <= :until")
    suspend fun deleteHeartRateDataBefore(until: Long)

    override suspend fun deleteDataBefore(timestamp: Long) = deleteHeartRateDataBefore(timestamp)

    @Query("DELETE FROM HeartRateEntity")
    suspend fun deleteAllHeartRateData()

    override suspend fun deleteAll() {
        deleteAllHeartRateData()
    }
}