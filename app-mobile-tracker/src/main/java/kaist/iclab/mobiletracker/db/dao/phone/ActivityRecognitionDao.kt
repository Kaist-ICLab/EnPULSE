package kaist.iclab.mobiletracker.db.dao.phone

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kaist.iclab.mobiletracker.db.dao.common.BaseDao
import kaist.iclab.mobiletracker.db.entity.phone.ActivityRecognitionEntity
import kaist.iclab.tracker.sensor.common.ActivityRecognitionSensor

@Dao
interface ActivityRecognitionDao : BaseDao<ActivityRecognitionSensor.Entity, ActivityRecognitionEntity> {
    override suspend fun insert(sensorEntity: ActivityRecognitionSensor.Entity, userUuid: String?) {
        val entity = ActivityRecognitionEntity(
            uuid = userUuid ?: "",
            received = sensorEntity.received,
            timestamp = sensorEntity.timestamp,
            elapsedRealtimeMillis = sensorEntity.elapsedRealtimeMillis,
            activityType = sensorEntity.activityType,
            score = sensorEntity.score,
            probabilities = sensorEntity.probabilities
        )
        insertUsingRoomEntity(entity)
    }

    @Insert
    suspend fun insertUsingRoomEntity(entity: ActivityRecognitionEntity)

    @Insert
    suspend fun insertBatchUsingRoomEntity(entities: List<ActivityRecognitionEntity>)

    override suspend fun insertBatch(entities: List<ActivityRecognitionSensor.Entity>, userUuid: String?) {
        val roomEntities = entities.map { entity ->
            ActivityRecognitionEntity(
                uuid = userUuid ?: "",
                received = entity.received,
                timestamp = entity.timestamp,
                elapsedRealtimeMillis = entity.elapsedRealtimeMillis,
                activityType = entity.activityType,
                score = entity.score,
                probabilities = entity.probabilities
            )
        }
        insertBatchUsingRoomEntity(roomEntities)
    }

    @Query("SELECT * FROM ActivityRecognitionEntity WHERE timestamp > :afterTimestamp ORDER BY timestamp ASC")
    override suspend fun getDataAfterTimestamp(afterTimestamp: Long): List<ActivityRecognitionEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM ActivityRecognitionEntity WHERE timestamp > :afterTimestamp)")
    override suspend fun hasDataAfterTimestamp(afterTimestamp: Long): Boolean

    @Query("SELECT MAX(timestamp) FROM ActivityRecognitionEntity")
    override suspend fun getLatestTimestamp(): Long?

    @Query("SELECT COUNT(*) FROM ActivityRecognitionEntity")
    override suspend fun getRecordCount(): Int

    @Query("SELECT COUNT(*) FROM ActivityRecognitionEntity WHERE timestamp >= :afterTimestamp")
    override suspend fun getRecordCountAfterTimestamp(afterTimestamp: Long): Int

    @Query("SELECT * FROM ActivityRecognitionEntity WHERE timestamp >= :afterTimestamp ORDER BY CASE WHEN :isAscending = 1 THEN timestamp END ASC, CASE WHEN :isAscending = 0 THEN timestamp END DESC LIMIT :limit OFFSET :offset")
    override suspend fun getRecordsPaginated(
        afterTimestamp: Long,
        isAscending: Boolean,
        limit: Int,
        offset: Int
    ): List<ActivityRecognitionEntity>

    @Query("DELETE FROM ActivityRecognitionEntity WHERE id = :recordId")
    override suspend fun deleteById(recordId: Long)

    @Query("SELECT eventId FROM ActivityRecognitionEntity WHERE id = :recordId")
    override suspend fun getEventIdById(recordId: Long): String?

    @Query("DELETE FROM ActivityRecognitionEntity WHERE timestamp < :timestamp")
    override suspend fun deleteDataBefore(timestamp: Long)

    @Query("DELETE FROM ActivityRecognitionEntity")
    suspend fun deleteAllActivityRecognitionData()

    override suspend fun deleteAll() {
        deleteAllActivityRecognitionData()
    }
}
