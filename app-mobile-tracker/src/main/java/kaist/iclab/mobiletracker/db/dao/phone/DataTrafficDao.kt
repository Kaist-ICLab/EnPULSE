package kaist.iclab.mobiletracker.db.dao.phone

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kaist.iclab.mobiletracker.db.dao.common.BaseDao
import kaist.iclab.mobiletracker.db.entity.phone.DataTrafficEntity
import kaist.iclab.tracker.sensor.phone.DataTrafficSensor

@Dao
interface DataTrafficDao: BaseDao<DataTrafficSensor.Entity, DataTrafficEntity> {
    override suspend fun insert(sensorEntity: DataTrafficSensor.Entity, userUuid: String?) {
        val entity = DataTrafficEntity(
            uuid = userUuid ?: "",
            received = sensorEntity.received,
            timestamp = sensorEntity.timestamp,
            totalRx = sensorEntity.totalRx,
            totalTx = sensorEntity.totalTx,
            mobileRx = sensorEntity.mobileRx,
            mobileTx = sensorEntity.mobileTx
        )
        insertUsingRoomEntity(entity)
    }

    @Insert
    suspend fun insertUsingRoomEntity(dataTrafficEntity: DataTrafficEntity)

    @Insert
    suspend fun insertBatchUsingRoomEntity(entities: List<DataTrafficEntity>)

    override suspend fun insertBatch(entities: List<DataTrafficSensor.Entity>, userUuid: String?) {
        val roomEntities = entities.map { entity ->
            DataTrafficEntity(
                uuid = userUuid ?: "",
                received = entity.received,
                timestamp = entity.timestamp,
                totalRx = entity.totalRx,
                totalTx = entity.totalTx,
                mobileRx = entity.mobileRx,
                mobileTx = entity.mobileTx
            )
        }
        insertBatchUsingRoomEntity(roomEntities)
    }

    @Query("SELECT * FROM DataTrafficEntity ORDER BY timestamp ASC")
    suspend fun getAllDataTrafficData(): List<DataTrafficEntity>

    @Query("SELECT * FROM DataTrafficEntity WHERE timestamp > :afterTimestamp ORDER BY timestamp ASC")
    override suspend fun getDataAfterTimestamp(afterTimestamp: Long): List<DataTrafficEntity>

    @Query("SELECT MAX(timestamp) FROM DataTrafficEntity")
    override suspend fun getLatestTimestamp(): Long?

    @Query("SELECT COUNT(*) FROM DataTrafficEntity")
    override suspend fun getRecordCount(): Int

    @Query("SELECT COUNT(*) FROM DataTrafficEntity WHERE timestamp >= :afterTimestamp")
    suspend fun getRecordCountAfterTimestamp(afterTimestamp: Long): Int

    @Query("SELECT * FROM DataTrafficEntity WHERE timestamp >= :afterTimestamp ORDER BY CASE WHEN :isAscending = 1 THEN timestamp END ASC, CASE WHEN :isAscending = 0 THEN timestamp END DESC LIMIT :limit OFFSET :offset")
    suspend fun getRecordsPaginated(afterTimestamp: Long, isAscending: Boolean, limit: Int, offset: Int): List<DataTrafficEntity>

    @Query("DELETE FROM DataTrafficEntity WHERE id = :recordId")
    suspend fun deleteById(recordId: Long)

    @Query("SELECT eventId FROM DataTrafficEntity WHERE id = :recordId")
    suspend fun getEventIdById(recordId: Long): String?

    @Query("DELETE FROM DataTrafficEntity")
    suspend fun deleteAllDataTrafficData()

    @Query("SELECT COUNT(*) FROM DataTrafficEntity WHERE timestamp >= :afterTimestamp")
    fun getDailyDataTrafficCount(afterTimestamp: Long): kotlinx.coroutines.flow.Flow<Int>

    override suspend fun deleteAll() {
        deleteAllDataTrafficData()
    }
}
