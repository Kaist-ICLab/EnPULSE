package kaist.iclab.wearabletracker.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kaist.iclab.tracker.sensor.galaxywatch.SkinTemperatureSensor
import kaist.iclab.wearabletracker.db.entity.CsvSerializable
import kaist.iclab.wearabletracker.db.entity.SkinTemperatureEntity

@Dao
interface SkinTemperatureDao : BaseDao<SkinTemperatureSensor.Entity> {
    override suspend fun insert(sensorEntity: SkinTemperatureSensor.Entity) {
        val entity = sensorEntity.dataPoint.map {
            SkinTemperatureEntity(
                received = it.received,
                timestamp = it.timestamp,
                objectTemperature = it.objectTemperature,
                ambientTemperature = it.ambientTemperature,
                status = it.status
            )
        }
        insertUsingRoomEntity(entity)
    }

    override suspend fun insert(sensorEntities: List<SkinTemperatureSensor.Entity>) {
        val entities = sensorEntities.flatMap { sensorEntity ->
            sensorEntity.dataPoint.map {
                SkinTemperatureEntity(
                    received = it.received,
                    timestamp = it.timestamp,
                    objectTemperature = it.objectTemperature,
                    ambientTemperature = it.ambientTemperature,
                    status = it.status
                )
            }
        }
        insertUsingRoomEntity(entities)
    }

    @Insert
    suspend fun insertUsingRoomEntity(skinTemperatureEntity: List<SkinTemperatureEntity>)

    @Query("SELECT * FROM SkinTemperatureEntity ORDER BY timestamp ASC")
    suspend fun getAllSkinTemperatureData(): List<SkinTemperatureEntity>

    override suspend fun getAllForExport(): List<CsvSerializable> = getAllSkinTemperatureData()

    @Query("SELECT * FROM SkinTemperatureEntity WHERE timestamp > :since ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getSkinTemperatureDataSince(since: Long, limit: Int): List<SkinTemperatureEntity>

    override suspend fun getDataSince(timestamp: Long, limit: Int): List<CsvSerializable> =
        getSkinTemperatureDataSince(timestamp, limit)

    @Query("DELETE FROM SkinTemperatureEntity WHERE timestamp <= :until")
    suspend fun deleteSkinTemperatureDataBefore(until: Long)

    override suspend fun deleteDataBefore(timestamp: Long) =
        deleteSkinTemperatureDataBefore(timestamp)

    @Query("DELETE FROM SkinTemperatureEntity")
    suspend fun deleteAllSkinTemperatureData()

    override suspend fun deleteAll() {
        deleteAllSkinTemperatureData()
    }
}