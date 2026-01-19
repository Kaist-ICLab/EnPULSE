package kaist.iclab.mobiletracker.db.dao.watch

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kaist.iclab.mobiletracker.db.dao.common.BaseDao
import kaist.iclab.mobiletracker.db.entity.watch.WatchPPGEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchPPGDao : BaseDao<WatchPPGEntity, WatchPPGEntity> {
    @Insert
    suspend fun insert(entities: List<WatchPPGEntity>)
    
    override suspend fun insert(sensorEntity: WatchPPGEntity, userUuid: String?) {
        insert(listOf(sensorEntity))
    }

    override suspend fun insertBatch(entities: List<WatchPPGEntity>, userUuid: String?) {
        insert(entities)
    }

    @Query("SELECT * FROM watch_ppg ORDER BY timestamp ASC")
    suspend fun getAllPPGData(): List<WatchPPGEntity>

    @Query("SELECT * FROM watch_ppg WHERE timestamp > :afterTimestamp ORDER BY timestamp ASC")
    override suspend fun getDataAfterTimestamp(afterTimestamp: Long): List<WatchPPGEntity>

    @Query("SELECT MAX(timestamp) FROM watch_ppg")
    override suspend fun getLatestTimestamp(): Long?

    @Query("SELECT COUNT(*) FROM watch_ppg")
    override suspend fun getRecordCount(): Int

    @Query("SELECT COUNT(*) FROM watch_ppg WHERE timestamp >= :afterTimestamp")
    fun getDailyPPGCount(afterTimestamp: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM watch_ppg WHERE timestamp >= :afterTimestamp")
    suspend fun getRecordCountAfterTimestamp(afterTimestamp: Long): Int

    @Query("SELECT * FROM watch_ppg WHERE timestamp >= :afterTimestamp ORDER BY CASE WHEN :isAscending = 1 THEN timestamp END ASC, CASE WHEN :isAscending = 0 THEN timestamp END DESC LIMIT :limit OFFSET :offset")
    suspend fun getRecordsPaginated(afterTimestamp: Long, isAscending: Boolean, limit: Int, offset: Int): List<WatchPPGEntity>

    @Query("DELETE FROM watch_ppg WHERE id = :recordId")
    suspend fun deleteById(recordId: Long)

    @Query("SELECT eventId FROM watch_ppg WHERE id = :recordId")
    suspend fun getEventIdById(recordId: Long): String?

    @Query("DELETE FROM watch_ppg")
    override suspend fun deleteAll()
}
