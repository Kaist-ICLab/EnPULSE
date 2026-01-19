package kaist.iclab.mobiletracker.db.dao.watch

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kaist.iclab.mobiletracker.db.dao.common.BaseDao
import kaist.iclab.mobiletracker.db.entity.watch.WatchHeartRateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchHeartRateDao : BaseDao<WatchHeartRateEntity, WatchHeartRateEntity> {
    @Insert
    suspend fun insert(entities: List<WatchHeartRateEntity>)
    
    override suspend fun insert(sensorEntity: WatchHeartRateEntity, userUuid: String?) {
        insert(listOf(sensorEntity))
    }

    override suspend fun insertBatch(entities: List<WatchHeartRateEntity>, userUuid: String?) {
        insert(entities)
    }

    @Query("SELECT * FROM watch_heart_rate ORDER BY timestamp ASC")
    suspend fun getAllHeartRateData(): List<WatchHeartRateEntity>

    @Query("SELECT * FROM watch_heart_rate WHERE timestamp > :afterTimestamp ORDER BY timestamp ASC")
    override suspend fun getDataAfterTimestamp(afterTimestamp: Long): List<WatchHeartRateEntity>

    @Query("SELECT MAX(timestamp) FROM watch_heart_rate")
    override suspend fun getLatestTimestamp(): Long?

    @Query("SELECT COUNT(*) FROM watch_heart_rate")
    override suspend fun getRecordCount(): Int

    @Query("SELECT COUNT(*) FROM watch_heart_rate WHERE timestamp >= :afterTimestamp")
    fun getDailyHeartRateCount(afterTimestamp: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM watch_heart_rate WHERE timestamp >= :afterTimestamp")
    suspend fun getRecordCountAfterTimestamp(afterTimestamp: Long): Int

    @Query("SELECT * FROM watch_heart_rate WHERE timestamp >= :afterTimestamp ORDER BY CASE WHEN :isAscending = 1 THEN timestamp END ASC, CASE WHEN :isAscending = 0 THEN timestamp END DESC LIMIT :limit OFFSET :offset")
    suspend fun getRecordsPaginated(afterTimestamp: Long, isAscending: Boolean, limit: Int, offset: Int): List<WatchHeartRateEntity>

    @Query("DELETE FROM watch_heart_rate WHERE id = :recordId")
    suspend fun deleteById(recordId: Long)

    @Query("SELECT eventId FROM watch_heart_rate WHERE id = :recordId")
    suspend fun getEventIdById(recordId: Long): String?

    @Query("DELETE FROM watch_heart_rate")
    override suspend fun deleteAll()
}
