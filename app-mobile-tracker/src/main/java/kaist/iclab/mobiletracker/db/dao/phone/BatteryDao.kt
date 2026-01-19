package kaist.iclab.mobiletracker.db.dao.phone

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kaist.iclab.mobiletracker.db.dao.common.BaseDao
import kaist.iclab.mobiletracker.db.entity.phone.BatteryEntity
import kaist.iclab.tracker.sensor.phone.BatterySensor
import kotlinx.coroutines.flow.Flow

@Dao
interface BatteryDao: BaseDao<BatterySensor.Entity, BatteryEntity> {
    override suspend fun insert(sensorEntity: BatterySensor.Entity, userUuid: String?) {
        val entity = BatteryEntity(
            uuid = userUuid ?: "",
            received = sensorEntity.received,
            timestamp = sensorEntity.timestamp,
            connectedType = sensorEntity.connectedType,
            status = sensorEntity.status,
            level = sensorEntity.level,
            temperature = sensorEntity.temperature
        )
        insertUsingRoomEntity(entity)
    }

    @Insert
    suspend fun insertUsingRoomEntity(batteryEntity: BatteryEntity)

    @Insert
    suspend fun insertBatchUsingRoomEntity(entities: List<BatteryEntity>)

    override suspend fun insertBatch(entities: List<BatterySensor.Entity>, userUuid: String?) {
        val roomEntities = entities.map { entity ->
            BatteryEntity(
                uuid = userUuid ?: "",
                received = entity.received,
                timestamp = entity.timestamp,
                connectedType = entity.connectedType,
                status = entity.status,
                level = entity.level,
                temperature = entity.temperature
            )
        }
        insertBatchUsingRoomEntity(roomEntities)
    }

    @Query("SELECT * FROM BatteryEntity ORDER BY timestamp ASC")
    suspend fun getAllBatteryData(): List<BatteryEntity>

    @Query("SELECT * FROM BatteryEntity WHERE timestamp > :afterTimestamp ORDER BY timestamp ASC")
    override suspend fun getDataAfterTimestamp(afterTimestamp: Long): List<BatteryEntity>

    @Query("SELECT COUNT(*) FROM BatteryEntity WHERE timestamp >= :afterTimestamp")
    fun getDailyBatteryCount(afterTimestamp: Long): Flow<Int>

    @Query("SELECT MAX(timestamp) FROM BatteryEntity")
    override suspend fun getLatestTimestamp(): Long?

    @Query("SELECT COUNT(*) FROM BatteryEntity")
    override suspend fun getRecordCount(): Int

    @Query("SELECT COUNT(*) FROM BatteryEntity WHERE timestamp >= :afterTimestamp")
    suspend fun getRecordCountAfterTimestamp(afterTimestamp: Long): Int

    @Query("SELECT * FROM BatteryEntity WHERE timestamp >= :afterTimestamp ORDER BY CASE WHEN :isAscending = 1 THEN timestamp END ASC, CASE WHEN :isAscending = 0 THEN timestamp END DESC LIMIT :limit OFFSET :offset")
    suspend fun getRecordsPaginated(afterTimestamp: Long, isAscending: Boolean, limit: Int, offset: Int): List<BatteryEntity>

    @Query("DELETE FROM BatteryEntity WHERE id = :recordId")
    suspend fun deleteById(recordId: Long)

    @Query("SELECT eventId FROM BatteryEntity WHERE id = :recordId")
    suspend fun getEventIdById(recordId: Long): String?

    @Query("DELETE FROM BatteryEntity")
    suspend fun deleteAllBatteryData()

    override suspend fun deleteAll() {
        deleteAllBatteryData()
    }
}

