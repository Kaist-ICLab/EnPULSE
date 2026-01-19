package kaist.iclab.wearabletracker.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kaist.iclab.tracker.sensor.common.LocationSensor
import kaist.iclab.wearabletracker.db.entity.CsvSerializable
import kaist.iclab.wearabletracker.db.entity.LocationEntity

@Dao
interface LocationDao : BaseDao<LocationSensor.Entity> {
    override suspend fun insert(sensorEntity: LocationSensor.Entity) {
        val entity = LocationEntity(
            received = sensorEntity.received,
            timestamp = sensorEntity.timestamp,
            latitude = sensorEntity.latitude,
            longitude = sensorEntity.longitude,
            altitude = sensorEntity.altitude,
            speed = sensorEntity.speed,
            accuracy = sensorEntity.accuracy
        )
        insertUsingRoomEntity(entity)
    }

    @Insert
    suspend fun insertUsingRoomEntity(locationEntity: LocationEntity)

    @Query("SELECT * FROM LocationEntity ORDER BY timestamp ASC")
    suspend fun getAllLocationData(): List<LocationEntity>

    override suspend fun getAllForExport(): List<CsvSerializable> = getAllLocationData()

    @Query("SELECT * FROM LocationEntity WHERE timestamp > :since ORDER BY timestamp ASC")
    suspend fun getLocationDataSince(since: Long): List<LocationEntity>

    override suspend fun getDataSince(timestamp: Long): List<CsvSerializable> =
        getLocationDataSince(timestamp)

    @Query("DELETE FROM LocationEntity WHERE timestamp <= :until")
    suspend fun deleteLocationDataBefore(until: Long)

    override suspend fun deleteDataBefore(timestamp: Long) = deleteLocationDataBefore(timestamp)

    @Query("DELETE FROM LocationEntity")
    suspend fun deleteAllLocationData()

    override suspend fun deleteAll() {
        deleteAllLocationData()
    }
}