package kaist.iclab.mobiletracker.db.dao.watch

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kaist.iclab.mobiletracker.db.dao.common.BaseDao
import kaist.iclab.mobiletracker.db.entity.watch.WatchIMUEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchIMUDao : BaseDao<WatchIMUEntity, WatchIMUEntity> {
    @Insert
    suspend fun insert(entities: List<WatchIMUEntity>)

    override suspend fun insert(sensorEntity: WatchIMUEntity, userUuid: String?) {
        insert(listOf(sensorEntity.copy(uuid = userUuid)))
    }

    override suspend fun insertBatch(entities: List<WatchIMUEntity>, userUuid: String?) {
        insert(entities.map { it.copy(uuid = userUuid) })
    }

    @Query("SELECT * FROM WatchIMUEntity WHERE timestamp > :afterTimestamp ORDER BY timestamp ASC")
    override suspend fun getDataAfterTimestamp(afterTimestamp: Long): List<WatchIMUEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM WatchIMUEntity WHERE timestamp > :afterTimestamp)")
    override suspend fun hasDataAfterTimestamp(afterTimestamp: Long): Boolean

    @Query("SELECT MAX(timestamp) FROM WatchIMUEntity")
    override suspend fun getLatestTimestamp(): Long?

    @Query("SELECT COUNT(*) FROM WatchIMUEntity")
    override suspend fun getRecordCount(): Int

    @Query("SELECT COUNT(*) FROM WatchIMUEntity WHERE timestamp >= :afterTimestamp")
    fun getDailyIMUCount(afterTimestamp: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM WatchIMUEntity WHERE timestamp >= :afterTimestamp")
    override suspend fun getRecordCountAfterTimestamp(afterTimestamp: Long): Int

    @Query("SELECT * FROM WatchIMUEntity WHERE timestamp >= :afterTimestamp ORDER BY CASE WHEN :isAscending = 1 THEN timestamp END ASC, CASE WHEN :isAscending = 0 THEN timestamp END DESC LIMIT :limit OFFSET :offset")
    override suspend fun getRecordsPaginated(
        afterTimestamp: Long,
        isAscending: Boolean,
        limit: Int,
        offset: Int
    ): List<WatchIMUEntity>

    @Query("DELETE FROM WatchIMUEntity WHERE id = :recordId")
    override suspend fun deleteById(recordId: Long)

    @Query("SELECT eventId FROM WatchIMUEntity WHERE id = :recordId")
    override suspend fun getEventIdById(recordId: Long): String?

    @Query("DELETE FROM WatchIMUEntity WHERE timestamp < :timestamp")
    override suspend fun deleteDataBefore(timestamp: Long)

    @Query("DELETE FROM WatchIMUEntity")
    override suspend fun deleteAll()
}
