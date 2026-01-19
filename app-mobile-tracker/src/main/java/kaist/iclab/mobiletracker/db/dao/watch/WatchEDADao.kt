package kaist.iclab.mobiletracker.db.dao.watch

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kaist.iclab.mobiletracker.db.dao.common.BaseDao
import kaist.iclab.mobiletracker.db.entity.watch.WatchEDAEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchEDADao : BaseDao<WatchEDAEntity, WatchEDAEntity> {
    @Insert
    suspend fun insert(entities: List<WatchEDAEntity>)
    
    override suspend fun insert(sensorEntity: WatchEDAEntity, userUuid: String?) {
        insert(listOf(sensorEntity))
    }

    override suspend fun insertBatch(entities: List<WatchEDAEntity>, userUuid: String?) {
        insert(entities)
    }

    @Query("SELECT * FROM watch_eda ORDER BY timestamp ASC")
    suspend fun getAllEDAData(): List<WatchEDAEntity>

    @Query("SELECT * FROM watch_eda WHERE timestamp > :afterTimestamp ORDER BY timestamp ASC")
    override suspend fun getDataAfterTimestamp(afterTimestamp: Long): List<WatchEDAEntity>

    @Query("SELECT MAX(timestamp) FROM watch_eda")
    override suspend fun getLatestTimestamp(): Long?

    @Query("SELECT COUNT(*) FROM watch_eda")
    override suspend fun getRecordCount(): Int

    @Query("SELECT COUNT(*) FROM watch_eda WHERE timestamp >= :afterTimestamp")
    fun getDailyEDACount(afterTimestamp: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM watch_eda WHERE timestamp >= :afterTimestamp")
    suspend fun getRecordCountAfterTimestamp(afterTimestamp: Long): Int

    @Query("SELECT * FROM watch_eda WHERE timestamp >= :afterTimestamp ORDER BY CASE WHEN :isAscending = 1 THEN timestamp END ASC, CASE WHEN :isAscending = 0 THEN timestamp END DESC LIMIT :limit OFFSET :offset")
    suspend fun getRecordsPaginated(afterTimestamp: Long, isAscending: Boolean, limit: Int, offset: Int): List<WatchEDAEntity>

    @Query("DELETE FROM watch_eda WHERE id = :recordId")
    suspend fun deleteById(recordId: Long)

    @Query("SELECT eventId FROM watch_eda WHERE id = :recordId")
    suspend fun getEventIdById(recordId: Long): String?

    @Query("DELETE FROM watch_eda")
    override suspend fun deleteAll()
}
