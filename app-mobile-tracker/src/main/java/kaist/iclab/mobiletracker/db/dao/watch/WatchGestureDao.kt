package kaist.iclab.mobiletracker.db.dao.watch

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kaist.iclab.mobiletracker.db.dao.common.BaseDao
import kaist.iclab.mobiletracker.db.entity.watch.WatchGestureEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchGestureDao : BaseDao<WatchGestureEntity, WatchGestureEntity> {
    @Insert
    suspend fun insert(entities: List<WatchGestureEntity>)

    override suspend fun insert(sensorEntity: WatchGestureEntity, userUuid: String?) {
        insert(listOf(sensorEntity.copy(uuid = userUuid)))
    }

    override suspend fun insertBatch(entities: List<WatchGestureEntity>, userUuid: String?) {
        insert(entities.map { it.copy(uuid = userUuid) })
    }

    @Query("SELECT * FROM WatchGestureEntity WHERE timestamp > :afterTimestamp ORDER BY timestamp ASC")
    override suspend fun getDataAfterTimestamp(afterTimestamp: Long): List<WatchGestureEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM WatchGestureEntity WHERE timestamp > :afterTimestamp)")
    override suspend fun hasDataAfterTimestamp(afterTimestamp: Long): Boolean

    @Query("SELECT MAX(timestamp) FROM WatchGestureEntity")
    override suspend fun getLatestTimestamp(): Long?

    @Query("SELECT COUNT(*) FROM WatchGestureEntity")
    override suspend fun getRecordCount(): Int

    @Query("SELECT COUNT(*) FROM WatchGestureEntity WHERE timestamp >= :afterTimestamp")
    fun getDailyGestureCount(afterTimestamp: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM WatchGestureEntity WHERE timestamp >= :afterTimestamp")
    override suspend fun getRecordCountAfterTimestamp(afterTimestamp: Long): Int

    @Query("SELECT * FROM WatchGestureEntity WHERE timestamp >= :afterTimestamp ORDER BY CASE WHEN :isAscending = 1 THEN timestamp END ASC, CASE WHEN :isAscending = 0 THEN timestamp END DESC LIMIT :limit OFFSET :offset")
    override suspend fun getRecordsPaginated(
        afterTimestamp: Long,
        isAscending: Boolean,
        limit: Int,
        offset: Int
    ): List<WatchGestureEntity>

    @Query("DELETE FROM WatchGestureEntity WHERE id = :recordId")
    override suspend fun deleteById(recordId: Long)

    @Query("SELECT eventId FROM WatchGestureEntity WHERE id = :recordId")
    override suspend fun getEventIdById(recordId: Long): String?

    @Query("DELETE FROM WatchGestureEntity WHERE timestamp < :timestamp")
    override suspend fun deleteDataBefore(timestamp: Long)

    @Query("DELETE FROM WatchGestureEntity")
    override suspend fun deleteAll()
}
