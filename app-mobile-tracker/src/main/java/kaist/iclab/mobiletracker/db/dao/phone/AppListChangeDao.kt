package kaist.iclab.mobiletracker.db.dao.phone

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.google.gson.Gson
import kaist.iclab.mobiletracker.db.dao.common.BaseDao
import kaist.iclab.mobiletracker.db.entity.phone.AppListChangeEntity
import kaist.iclab.tracker.sensor.phone.AppListChangeSensor

@Dao
interface AppListChangeDao: BaseDao<AppListChangeSensor.Entity, AppListChangeEntity> {
    companion object {
        private val gson = Gson()
    }

    override suspend fun insert(sensorEntity: AppListChangeSensor.Entity, userUuid: String?) {
        val entity = AppListChangeEntity(
            uuid = userUuid ?: "",
            received = sensorEntity.received,
            timestamp = sensorEntity.timestamp,
            changedAppJson = sensorEntity.changedApp?.let { gson.toJson(it) },
            appListJson = sensorEntity.appList?.let { gson.toJson(it) }
        )
        insertUsingRoomEntity(entity)
    }

    @Insert
    suspend fun insertUsingRoomEntity(appListChangeEntity: AppListChangeEntity)

    @Insert
    suspend fun insertBatchUsingRoomEntity(entities: List<AppListChangeEntity>)

    override suspend fun insertBatch(entities: List<AppListChangeSensor.Entity>, userUuid: String?) {
        val roomEntities = entities.map { entity ->
            AppListChangeEntity(
                uuid = userUuid ?: "",
                received = entity.received,
                timestamp = entity.timestamp,
                changedAppJson = entity.changedApp?.let { gson.toJson(it) },
                appListJson = entity.appList?.let { gson.toJson(it) }
            )
        }
        insertBatchUsingRoomEntity(roomEntities)
    }

    @Query("SELECT * FROM AppListChangeEntity ORDER BY timestamp ASC")
    suspend fun getAllAppListChangeData(): List<AppListChangeEntity>

    @Query("SELECT * FROM AppListChangeEntity WHERE timestamp > :afterTimestamp ORDER BY timestamp ASC")
    override suspend fun getDataAfterTimestamp(afterTimestamp: Long): List<AppListChangeEntity>

    @Query("SELECT MAX(timestamp) FROM AppListChangeEntity")
    override suspend fun getLatestTimestamp(): Long?

    @Query("SELECT COUNT(*) FROM AppListChangeEntity")
    override suspend fun getRecordCount(): Int

    @Query("SELECT COUNT(*) FROM AppListChangeEntity WHERE timestamp >= :afterTimestamp")
    suspend fun getRecordCountAfterTimestamp(afterTimestamp: Long): Int

    @Query("SELECT * FROM AppListChangeEntity WHERE timestamp >= :afterTimestamp ORDER BY CASE WHEN :isAscending = 1 THEN timestamp END ASC, CASE WHEN :isAscending = 0 THEN timestamp END DESC LIMIT :limit OFFSET :offset")
    suspend fun getRecordsPaginated(afterTimestamp: Long, isAscending: Boolean, limit: Int, offset: Int): List<AppListChangeEntity>

    @Query("DELETE FROM AppListChangeEntity WHERE id = :recordId")
    suspend fun deleteById(recordId: Long)

    @Query("SELECT eventId FROM AppListChangeEntity WHERE id = :recordId")
    suspend fun getEventIdById(recordId: Long): String?

    @Query("DELETE FROM AppListChangeEntity")
    suspend fun deleteAllAppListChangeData()

    @Query("SELECT COUNT(*) FROM AppListChangeEntity WHERE timestamp >= :afterTimestamp")
    fun getDailyAppListChangeCount(afterTimestamp: Long): kotlinx.coroutines.flow.Flow<Int>

    override suspend fun deleteAll() {
        deleteAllAppListChangeData()
    }
}
