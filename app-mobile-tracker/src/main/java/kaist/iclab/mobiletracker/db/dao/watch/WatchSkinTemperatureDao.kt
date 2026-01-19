package kaist.iclab.mobiletracker.db.dao.watch

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kaist.iclab.mobiletracker.db.dao.common.BaseDao
import kaist.iclab.mobiletracker.db.entity.watch.WatchSkinTemperatureEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchSkinTemperatureDao : BaseDao<WatchSkinTemperatureEntity, WatchSkinTemperatureEntity> {
    @Insert
    suspend fun insert(entities: List<WatchSkinTemperatureEntity>)
    
    override suspend fun insert(sensorEntity: WatchSkinTemperatureEntity, userUuid: String?) {
        insert(listOf(sensorEntity))
    }

    override suspend fun insertBatch(entities: List<WatchSkinTemperatureEntity>, userUuid: String?) {
        insert(entities)
    }

    @Query("SELECT * FROM watch_skin_temperature ORDER BY timestamp ASC")
    suspend fun getAllSkinTemperatureData(): List<WatchSkinTemperatureEntity>

    @Query("SELECT * FROM watch_skin_temperature WHERE timestamp > :afterTimestamp ORDER BY timestamp ASC")
    override suspend fun getDataAfterTimestamp(afterTimestamp: Long): List<WatchSkinTemperatureEntity>

    @Query("SELECT MAX(timestamp) FROM watch_skin_temperature")
    override suspend fun getLatestTimestamp(): Long?

    @Query("SELECT COUNT(*) FROM watch_skin_temperature")
    override suspend fun getRecordCount(): Int

    @Query("SELECT COUNT(*) FROM watch_skin_temperature WHERE timestamp >= :afterTimestamp")
    fun getDailySkinTemperatureCount(afterTimestamp: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM watch_skin_temperature WHERE timestamp >= :afterTimestamp")
    suspend fun getRecordCountAfterTimestamp(afterTimestamp: Long): Int

    @Query("SELECT * FROM watch_skin_temperature WHERE timestamp >= :afterTimestamp ORDER BY CASE WHEN :isAscending = 1 THEN timestamp END ASC, CASE WHEN :isAscending = 0 THEN timestamp END DESC LIMIT :limit OFFSET :offset")
    suspend fun getRecordsPaginated(afterTimestamp: Long, isAscending: Boolean, limit: Int, offset: Int): List<WatchSkinTemperatureEntity>

    @Query("DELETE FROM watch_skin_temperature WHERE id = :recordId")
    suspend fun deleteById(recordId: Long)

    @Query("SELECT eventId FROM watch_skin_temperature WHERE id = :recordId")
    suspend fun getEventIdById(recordId: Long): String?

    @Query("DELETE FROM watch_skin_temperature")
    override suspend fun deleteAll()
}
