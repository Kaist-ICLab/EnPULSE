package kaist.iclab.mobiletracker.db.dao.watch

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kaist.iclab.mobiletracker.db.dao.common.BaseDao
import kaist.iclab.mobiletracker.db.entity.watch.WatchStressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchStressDao : BaseDao<WatchStressEntity, WatchStressEntity> {
    @Insert
    suspend fun insert(entities: List<WatchStressEntity>)

    override suspend fun insert(sensorEntity: WatchStressEntity, userUuid: String?) {
        insert(listOf(sensorEntity.copy(uuid = userUuid)))
    }

    override suspend fun insertBatch(entities: List<WatchStressEntity>, userUuid: String?) {
        insert(entities.map { it.copy(uuid = userUuid) })
    }

    @Query("SELECT * FROM WatchStressEntity WHERE timestamp > :afterTimestamp ORDER BY timestamp ASC")
    override suspend fun getDataAfterTimestamp(afterTimestamp: Long): List<WatchStressEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM WatchStressEntity WHERE timestamp > :afterTimestamp)")
    override suspend fun hasDataAfterTimestamp(afterTimestamp: Long): Boolean

    @Query("SELECT MAX(timestamp) FROM WatchStressEntity")
    override suspend fun getLatestTimestamp(): Long?

    @Query("SELECT COUNT(*) FROM WatchStressEntity")
    override suspend fun getRecordCount(): Int

    @Query("SELECT COUNT(*) FROM WatchStressEntity WHERE timestamp >= :afterTimestamp")
    fun getDailyStressCount(afterTimestamp: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM WatchStressEntity WHERE timestamp >= :afterTimestamp")
    override suspend fun getRecordCountAfterTimestamp(afterTimestamp: Long): Int

    @Query("SELECT COUNT(*) FROM WatchStressEntity WHERE timestamp >= :startOfDay AND timestamp < :endOfDay")
    suspend fun getTodayCount(startOfDay: Long, endOfDay: Long): Int

    @Query("SELECT * FROM WatchStressEntity WHERE timestamp >= :afterTimestamp ORDER BY CASE WHEN :isAscending = 1 THEN timestamp END ASC, CASE WHEN :isAscending = 0 THEN timestamp END DESC LIMIT :limit OFFSET :offset")
    override suspend fun getRecordsPaginated(
        afterTimestamp: Long,
        isAscending: Boolean,
        limit: Int,
        offset: Int
    ): List<WatchStressEntity>

    @Query("DELETE FROM WatchStressEntity WHERE id = :recordId")
    override suspend fun deleteById(recordId: Long)

    @Query("SELECT eventId FROM WatchStressEntity WHERE id = :recordId")
    override suspend fun getEventIdById(recordId: Long): String?

    @Query("DELETE FROM WatchStressEntity WHERE timestamp < :timestamp")
    override suspend fun deleteDataBefore(timestamp: Long)

    @Query("DELETE FROM WatchStressEntity")
    override suspend fun deleteAll()
}
