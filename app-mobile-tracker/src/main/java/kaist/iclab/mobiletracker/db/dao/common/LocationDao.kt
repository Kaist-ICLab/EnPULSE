package kaist.iclab.mobiletracker.db.dao.common

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kaist.iclab.mobiletracker.data.DeviceType
import kaist.iclab.mobiletracker.db.entity.common.LocationEntity
import kaist.iclab.tracker.sensor.common.LocationSensor
import kotlinx.coroutines.flow.Flow

/**
 * Unified DAO for location sensor data from both phone and watch devices.
 * Supports both:
 * - Phone sensors: Converts from LocationSensor.Entity to LocationEntity
 * - Watch sensors: Works directly with LocationEntity (from CSV parsing)
 */
@Dao
interface LocationDao : BaseDao<LocationSensor.Entity, LocationEntity> {

    // Methods for phone sensors (converting from LocationSensor.Entity)
    override suspend fun insert(sensorEntity: LocationSensor.Entity, userUuid: String?) {
        val entity = LocationEntity(
            uuid = userUuid ?: "",
            deviceType = DeviceType.PHONE.value,
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

    override suspend fun insertBatch(entities: List<LocationSensor.Entity>, userUuid: String?) {
        val roomEntities = entities.map { entity ->
            LocationEntity(
                uuid = userUuid ?: "",
                deviceType = DeviceType.PHONE.value,
                received = entity.received,
                timestamp = entity.timestamp,
                latitude = entity.latitude,
                longitude = entity.longitude,
                altitude = entity.altitude,
                speed = entity.speed,
                accuracy = entity.accuracy
            )
        }
        insertBatchUsingRoomEntity(roomEntities)
    }

    // Methods for watch sensors (working directly with LocationEntity)
    @Insert
    suspend fun insertUsingRoomEntity(locationEntity: LocationEntity)

    @Insert
    suspend fun insertBatchUsingRoomEntity(entities: List<LocationEntity>)

    // Convenience method for watch sensors to insert a list directly
    suspend fun insertLocationEntities(entities: List<LocationEntity>) {
        if (entities.isNotEmpty()) {
            insertBatchUsingRoomEntity(entities)
        }
    }

    @Query("SELECT COUNT(*) FROM location WHERE timestamp >= :afterTimestamp")
    fun getDailyLocationCount(afterTimestamp: Long): Flow<Int>

    // Query methods
    @Query("SELECT * FROM location ORDER BY timestamp ASC")
    suspend fun getAllLocationData(): List<LocationEntity>

    @Query("SELECT * FROM location WHERE timestamp > :afterTimestamp ORDER BY timestamp ASC")
    override suspend fun getDataAfterTimestamp(afterTimestamp: Long): List<LocationEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM location WHERE timestamp > :afterTimestamp)")
    override suspend fun hasDataAfterTimestamp(afterTimestamp: Long): Boolean

    // Device-specific check methods
    @Query("SELECT EXISTS(SELECT 1 FROM location WHERE deviceType = :deviceType AND timestamp > :afterTimestamp)")
    suspend fun hasDataAfterTimestampByDeviceType(
        afterTimestamp: Long,
        deviceType: Int
    ): Boolean

    // Device-specific query methods
    @Query("SELECT * FROM location WHERE deviceType = :deviceType AND timestamp > :afterTimestamp ORDER BY timestamp ASC")
    suspend fun getDataAfterTimestampByDeviceType(
        afterTimestamp: Long,
        deviceType: Int
    ): List<LocationEntity>

    @Query("SELECT MAX(timestamp) FROM location")
    override suspend fun getLatestTimestamp(): Long?

    @Query("SELECT MAX(timestamp) FROM location WHERE deviceType = :deviceType")
    suspend fun getLatestTimestampByDeviceType(deviceType: Int): Long?

    @Query("SELECT COUNT(*) FROM location")
    override suspend fun getRecordCount(): Int

    @Query("SELECT COUNT(*) FROM location WHERE deviceType = :deviceType")
    suspend fun getRecordCountByDeviceType(deviceType: Int): Int

    @Query("SELECT COUNT(*) FROM location WHERE timestamp >= :afterTimestamp")
    override suspend fun getRecordCountAfterTimestamp(afterTimestamp: Long): Int

    @Query("SELECT * FROM location WHERE timestamp >= :afterTimestamp ORDER BY CASE WHEN :isAscending = 1 THEN timestamp END ASC, CASE WHEN :isAscending = 0 THEN timestamp END DESC LIMIT :limit OFFSET :offset")
    override suspend fun getRecordsPaginated(
        afterTimestamp: Long,
        isAscending: Boolean,
        limit: Int,
        offset: Int
    ): List<LocationEntity>

    @Query("SELECT * FROM location WHERE deviceType = :deviceType AND timestamp >= :afterTimestamp ORDER BY CASE WHEN :isAscending = 1 THEN timestamp END ASC, CASE WHEN :isAscending = 0 THEN timestamp END DESC LIMIT :limit OFFSET :offset")
    suspend fun getRecordsPaginatedByDeviceType(
        afterTimestamp: Long,
        isAscending: Boolean,
        limit: Int,
        offset: Int,
        deviceType: Int
    ): List<LocationEntity>

    @Query("DELETE FROM location WHERE id = :recordId")
    override suspend fun deleteById(recordId: Long)

    @Query("SELECT eventId FROM location WHERE id = :recordId")
    override suspend fun getEventIdById(recordId: Long): String?

    @Query("DELETE FROM location WHERE deviceType = :deviceType AND timestamp < :timestamp")
    suspend fun deleteDataBeforeByDeviceType(timestamp: Long, deviceType: Int)

    override suspend fun deleteDataBefore(timestamp: Long) {
        deleteDataBeforeByDeviceType(timestamp, DeviceType.PHONE.value)
    }

    @Query("DELETE FROM location")
    override suspend fun deleteAll()
}
