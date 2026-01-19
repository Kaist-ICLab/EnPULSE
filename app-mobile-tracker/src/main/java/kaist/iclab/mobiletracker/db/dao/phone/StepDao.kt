package kaist.iclab.mobiletracker.db.dao.phone

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kaist.iclab.mobiletracker.db.dao.common.BaseDao
import kaist.iclab.mobiletracker.db.entity.phone.StepEntity
import kaist.iclab.tracker.sensor.phone.StepSensor
import kotlinx.coroutines.flow.Flow

@Dao
interface StepDao : BaseDao<StepSensor.Entity, StepEntity> {
    override suspend fun insert(sensorEntity: StepSensor.Entity, userUuid: String?) {
        val entity = StepEntity(
            uuid = userUuid ?: "",
            received = sensorEntity.received,
            timestamp = sensorEntity.timestamp,
            startTime = sensorEntity.startTime,
            endTime = sensorEntity.endTime,
            steps = sensorEntity.steps
        )
        insertUsingRoomEntity(entity)
    }

    @Insert
    suspend fun insertUsingRoomEntity(entity: StepEntity)

    @Insert
    suspend fun insertBatchUsingRoomEntity(entities: List<StepEntity>)

    override suspend fun insertBatch(entities: List<StepSensor.Entity>, userUuid: String?) {
        val roomEntities = entities.map { e ->
            StepEntity(
                uuid = userUuid ?: "",
                received = e.received,
                timestamp = e.timestamp,
                startTime = e.startTime,
                endTime = e.endTime,
                steps = e.steps
            )
        }
        insertBatchUsingRoomEntity(roomEntities)
    }

    @Query("SELECT * FROM StepEntity ORDER BY timestamp ASC")
    suspend fun getAllStepData(): List<StepEntity>

    @Query("SELECT * FROM StepEntity WHERE timestamp > :afterTimestamp ORDER BY timestamp ASC")
    override suspend fun getDataAfterTimestamp(afterTimestamp: Long): List<StepEntity>

    @Query("SELECT COUNT(*) FROM StepEntity WHERE timestamp >= :afterTimestamp")
    fun getDailyStepCount(afterTimestamp: Long): Flow<Int>

    @Query("SELECT MAX(timestamp) FROM StepEntity")
    override suspend fun getLatestTimestamp(): Long?

    @Query("SELECT COUNT(*) FROM StepEntity")
    override suspend fun getRecordCount(): Int

    @Query("SELECT COUNT(*) FROM StepEntity WHERE timestamp >= :afterTimestamp")
    suspend fun getRecordCountAfterTimestamp(afterTimestamp: Long): Int

    @Query("SELECT * FROM StepEntity WHERE timestamp >= :afterTimestamp ORDER BY CASE WHEN :isAscending = 1 THEN timestamp END ASC, CASE WHEN :isAscending = 0 THEN timestamp END DESC LIMIT :limit OFFSET :offset")
    suspend fun getRecordsPaginated(afterTimestamp: Long, isAscending: Boolean, limit: Int, offset: Int): List<StepEntity>

    @Query("DELETE FROM StepEntity WHERE id = :recordId")
    suspend fun deleteById(recordId: Long)

    @Query("SELECT eventId FROM StepEntity WHERE id = :recordId")
    suspend fun getEventIdById(recordId: Long): String?

    @Query("DELETE FROM StepEntity")
    suspend fun deleteAllStepData()

    override suspend fun deleteAll() {
        deleteAllStepData()
    }
}

