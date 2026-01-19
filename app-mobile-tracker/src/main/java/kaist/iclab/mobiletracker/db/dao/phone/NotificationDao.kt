package kaist.iclab.mobiletracker.db.dao.phone

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kaist.iclab.mobiletracker.db.dao.common.BaseDao
import kaist.iclab.mobiletracker.db.entity.phone.NotificationEntity
import kaist.iclab.tracker.sensor.phone.NotificationSensor
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao : BaseDao<NotificationSensor.Entity, NotificationEntity> {
    override suspend fun insert(sensorEntity: NotificationSensor.Entity, userUuid: String?) {
        val entity = NotificationEntity(
            uuid = userUuid ?: "",
            received = sensorEntity.received,
            timestamp = sensorEntity.timestamp,
            packageName = sensorEntity.packageName,
            eventType = sensorEntity.eventType,
            title = sensorEntity.title,
            text = sensorEntity.text,
            visibility = sensorEntity.visibility,
            category = sensorEntity.category
        )
        insertUsingRoomEntity(entity)
    }

    @Insert
    suspend fun insertUsingRoomEntity(notificationEntity: NotificationEntity)

    @Insert
    suspend fun insertBatchUsingRoomEntity(entities: List<NotificationEntity>)

    override suspend fun insertBatch(entities: List<NotificationSensor.Entity>, userUuid: String?) {
        val roomEntities = entities.map { entity ->
            NotificationEntity(
                uuid = userUuid ?: "",
                received = entity.received,
                timestamp = entity.timestamp,
                packageName = entity.packageName,
                eventType = entity.eventType,
                title = entity.title,
                text = entity.text,
                visibility = entity.visibility,
                category = entity.category
            )
        }
        insertBatchUsingRoomEntity(roomEntities)
    }

    @Query("SELECT * FROM NotificationEntity ORDER BY timestamp ASC")
    suspend fun getAllNotificationData(): List<NotificationEntity>

    @Query("SELECT * FROM NotificationEntity WHERE timestamp > :afterTimestamp ORDER BY timestamp ASC")
    override suspend fun getDataAfterTimestamp(afterTimestamp: Long): List<NotificationEntity>

    @Query("SELECT COUNT(*) FROM NotificationEntity WHERE timestamp >= :afterTimestamp")
    fun getDailyNotificationCount(afterTimestamp: Long): Flow<Int>

    @Query("SELECT MAX(timestamp) FROM NotificationEntity")
    override suspend fun getLatestTimestamp(): Long?

    @Query("SELECT COUNT(*) FROM NotificationEntity")
    override suspend fun getRecordCount(): Int

    @Query("SELECT COUNT(*) FROM NotificationEntity WHERE timestamp >= :afterTimestamp")
    suspend fun getRecordCountAfterTimestamp(afterTimestamp: Long): Int

    @Query("SELECT * FROM NotificationEntity WHERE timestamp >= :afterTimestamp ORDER BY CASE WHEN :isAscending = 1 THEN timestamp END ASC, CASE WHEN :isAscending = 0 THEN timestamp END DESC LIMIT :limit OFFSET :offset")
    suspend fun getRecordsPaginated(afterTimestamp: Long, isAscending: Boolean, limit: Int, offset: Int): List<NotificationEntity>

    @Query("DELETE FROM NotificationEntity WHERE id = :recordId")
    suspend fun deleteById(recordId: Long)

    @Query("SELECT eventId FROM NotificationEntity WHERE id = :recordId")
    suspend fun getEventIdById(recordId: Long): String?

    @Query("DELETE FROM NotificationEntity")
    suspend fun deleteAllNotificationData()

    override suspend fun deleteAll() {
        deleteAllNotificationData()
    }
}
