package kaist.iclab.mobiletracker.db.dao.phone

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kaist.iclab.mobiletracker.db.dao.common.BaseDao
import kaist.iclab.mobiletracker.db.entity.phone.MediaEntity
import kaist.iclab.tracker.sensor.phone.MediaSensor

@Dao
interface MediaDao : BaseDao<MediaSensor.Entity, MediaEntity> {
    override suspend fun insert(sensorEntity: MediaSensor.Entity, userUuid: String?) {
        val entity = MediaEntity(
            uuid = userUuid ?: "",
            received = sensorEntity.received,
            timestamp = sensorEntity.timestamp,
            operation = sensorEntity.operation,
            mediaType = sensorEntity.mediaType,
            storageType = sensorEntity.storageType,
            uri = sensorEntity.uri,
            fileName = sensorEntity.fileName,
            mimeType = sensorEntity.mimeType,
            size = sensorEntity.size,
            dateAdded = sensorEntity.dateAdded,
            dateModified = sensorEntity.dateModified
        )
        insertUsingRoomEntity(entity)
    }

    @Insert
    suspend fun insertUsingRoomEntity(entity: MediaEntity)

    @Insert
    suspend fun insertBatchUsingRoomEntity(entities: List<MediaEntity>)

    override suspend fun insertBatch(entities: List<MediaSensor.Entity>, userUuid: String?) {
        val roomEntities = entities.map { e ->
            MediaEntity(
                uuid = userUuid ?: "",
                received = e.received,
                timestamp = e.timestamp,
                operation = e.operation,
                mediaType = e.mediaType,
                storageType = e.storageType,
                uri = e.uri,
                fileName = e.fileName,
                mimeType = e.mimeType,
                size = e.size,
                dateAdded = e.dateAdded,
                dateModified = e.dateModified
            )
        }
        insertBatchUsingRoomEntity(roomEntities)
    }

    @Query("SELECT * FROM MediaEntity ORDER BY timestamp ASC")
    suspend fun getAllMediaData(): List<MediaEntity>

    @Query("SELECT * FROM MediaEntity WHERE timestamp > :afterTimestamp ORDER BY timestamp ASC")
    override suspend fun getDataAfterTimestamp(afterTimestamp: Long): List<MediaEntity>

    @Query("SELECT MAX(timestamp) FROM MediaEntity")
    override suspend fun getLatestTimestamp(): Long?

    @Query("SELECT COUNT(*) FROM MediaEntity")
    override suspend fun getRecordCount(): Int

    @Query("SELECT COUNT(*) FROM MediaEntity WHERE timestamp >= :afterTimestamp")
    suspend fun getRecordCountAfterTimestamp(afterTimestamp: Long): Int

    @Query("SELECT * FROM MediaEntity WHERE timestamp >= :afterTimestamp ORDER BY CASE WHEN :isAscending = 1 THEN timestamp END ASC, CASE WHEN :isAscending = 0 THEN timestamp END DESC LIMIT :limit OFFSET :offset")
    suspend fun getRecordsPaginated(afterTimestamp: Long, isAscending: Boolean, limit: Int, offset: Int): List<MediaEntity>

    @Query("DELETE FROM MediaEntity WHERE id = :recordId")
    suspend fun deleteById(recordId: Long)

    @Query("SELECT eventId FROM MediaEntity WHERE id = :recordId")
    suspend fun getEventIdById(recordId: Long): String?

    @Query("DELETE FROM MediaEntity")
    suspend fun deleteAllMediaData()

    @Query("SELECT COUNT(*) FROM MediaEntity WHERE timestamp >= :afterTimestamp")
    fun getDailyMediaCount(afterTimestamp: Long): kotlinx.coroutines.flow.Flow<Int>

    override suspend fun deleteAll() {
        deleteAllMediaData()
    }
}

