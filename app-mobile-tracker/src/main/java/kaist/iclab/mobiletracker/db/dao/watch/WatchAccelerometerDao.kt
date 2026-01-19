package kaist.iclab.mobiletracker.db.dao.watch

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kaist.iclab.mobiletracker.db.dao.common.BaseDao
import kaist.iclab.mobiletracker.db.entity.watch.WatchAccelerometerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchAccelerometerDao : BaseDao<WatchAccelerometerEntity, WatchAccelerometerEntity> {
    @Insert
    suspend fun insert(entities: List<WatchAccelerometerEntity>)
    
    override suspend fun insert(sensorEntity: WatchAccelerometerEntity, userUuid: String?) {
        insert(listOf(sensorEntity))
    }

    override suspend fun insertBatch(entities: List<WatchAccelerometerEntity>, userUuid: String?) {
        insert(entities)
    }

    @Query("SELECT * FROM watch_accelerometer ORDER BY timestamp ASC")
    suspend fun getAllAccelerometerData(): List<WatchAccelerometerEntity>

    @Query("SELECT * FROM watch_accelerometer WHERE timestamp > :afterTimestamp ORDER BY timestamp ASC")
    override suspend fun getDataAfterTimestamp(afterTimestamp: Long): List<WatchAccelerometerEntity>

    @Query("SELECT MAX(timestamp) FROM watch_accelerometer")
    override suspend fun getLatestTimestamp(): Long?

    @Query("SELECT COUNT(*) FROM watch_accelerometer")
    override suspend fun getRecordCount(): Int

    @Query("SELECT COUNT(*) FROM watch_accelerometer WHERE timestamp >= :afterTimestamp")
    fun getDailyAccelerometerCount(afterTimestamp: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM watch_accelerometer WHERE timestamp >= :afterTimestamp")
    suspend fun getRecordCountAfterTimestamp(afterTimestamp: Long): Int

    @Query("SELECT * FROM watch_accelerometer WHERE timestamp >= :afterTimestamp ORDER BY CASE WHEN :isAscending = 1 THEN timestamp END ASC, CASE WHEN :isAscending = 0 THEN timestamp END DESC LIMIT :limit OFFSET :offset")
    suspend fun getRecordsPaginated(afterTimestamp: Long, isAscending: Boolean, limit: Int, offset: Int): List<WatchAccelerometerEntity>

    @Query("DELETE FROM watch_accelerometer WHERE id = :recordId")
    suspend fun deleteById(recordId: Long)

    @Query("SELECT eventId FROM watch_accelerometer WHERE id = :recordId")
    suspend fun getEventIdById(recordId: Long): String?

    @Query("DELETE FROM watch_accelerometer")
    override suspend fun deleteAll()
}
