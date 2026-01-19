package kaist.iclab.mobiletracker.db.dao.phone

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kaist.iclab.mobiletracker.db.dao.common.BaseDao
import kaist.iclab.mobiletracker.db.entity.phone.ScreenEntity
import kaist.iclab.tracker.sensor.phone.ScreenSensor
import kotlinx.coroutines.flow.Flow

@Dao
interface ScreenDao: BaseDao<ScreenSensor.Entity, ScreenEntity> {
    override suspend fun insert(sensorEntity: ScreenSensor.Entity, userUuid: String?) {
        val entity = ScreenEntity(
            uuid = userUuid ?: "",
            received = sensorEntity.received,
            timestamp = sensorEntity.timestamp,
            type = sensorEntity.type
        )
        insertUsingRoomEntity(entity)
    }

    @Insert
    suspend fun insertUsingRoomEntity(screenEntity: ScreenEntity)

    @Insert
    suspend fun insertBatchUsingRoomEntity(entities: List<ScreenEntity>)

    override suspend fun insertBatch(entities: List<ScreenSensor.Entity>, userUuid: String?) {
        val roomEntities = entities.map { entity ->
            ScreenEntity(
                uuid = userUuid ?: "",
                received = entity.received,
                timestamp = entity.timestamp,
                type = entity.type
            )
        }
        insertBatchUsingRoomEntity(roomEntities)
    }

    @Query("SELECT * FROM ScreenEntity ORDER BY timestamp ASC")
    suspend fun getAllScreenData(): List<ScreenEntity>

    @Query("SELECT * FROM ScreenEntity WHERE timestamp > :afterTimestamp ORDER BY timestamp ASC")
    override suspend fun getDataAfterTimestamp(afterTimestamp: Long): List<ScreenEntity>

    @Query("SELECT COUNT(*) FROM ScreenEntity WHERE timestamp >= :afterTimestamp")
    fun getDailyScreenCount(afterTimestamp: Long): Flow<Int>

    @Query("SELECT MAX(timestamp) FROM ScreenEntity")
    override suspend fun getLatestTimestamp(): Long?

    @Query("SELECT COUNT(*) FROM ScreenEntity")
    override suspend fun getRecordCount(): Int

    @Query("SELECT COUNT(*) FROM ScreenEntity WHERE timestamp >= :afterTimestamp")
    suspend fun getRecordCountAfterTimestamp(afterTimestamp: Long): Int

    @Query("SELECT * FROM ScreenEntity WHERE timestamp >= :afterTimestamp ORDER BY CASE WHEN :isAscending = 1 THEN timestamp END ASC, CASE WHEN :isAscending = 0 THEN timestamp END DESC LIMIT :limit OFFSET :offset")
    suspend fun getRecordsPaginated(afterTimestamp: Long, isAscending: Boolean, limit: Int, offset: Int): List<ScreenEntity>

    @Query("DELETE FROM ScreenEntity WHERE id = :recordId")
    suspend fun deleteById(recordId: Long)

    @Query("SELECT eventId FROM ScreenEntity WHERE id = :recordId")
    suspend fun getEventIdById(recordId: Long): String?

    @Query("DELETE FROM ScreenEntity")
    suspend fun deleteAllScreenData()

    override suspend fun deleteAll() {
        deleteAllScreenData()
    }
}
