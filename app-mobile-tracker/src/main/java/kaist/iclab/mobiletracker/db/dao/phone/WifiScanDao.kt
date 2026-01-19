package kaist.iclab.mobiletracker.db.dao.phone

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kaist.iclab.mobiletracker.db.dao.common.BaseDao
import kaist.iclab.mobiletracker.db.entity.phone.WifiScanEntity
import kaist.iclab.tracker.sensor.phone.WifiScanSensor

@Dao
interface WifiScanDao: BaseDao<WifiScanSensor.Entity, WifiScanEntity> {
    override suspend fun insert(sensorEntity: WifiScanSensor.Entity, userUuid: String?) {
        val entity = WifiScanEntity(
            uuid = userUuid ?: "",
            received = sensorEntity.received,
            timestamp = sensorEntity.timestamp,
            ssid = sensorEntity.ssid,
            bssid = sensorEntity.bssid,
            frequency = sensorEntity.frequency,
            level = sensorEntity.level
        )
        insertUsingRoomEntity(entity)
    }

    @Insert
    suspend fun insertUsingRoomEntity(wifiScanEntity: WifiScanEntity)

    @Insert
    suspend fun insertBatchUsingRoomEntity(entities: List<WifiScanEntity>)

    override suspend fun insertBatch(entities: List<WifiScanSensor.Entity>, userUuid: String?) {
        val roomEntities = entities.map { entity ->
            WifiScanEntity(
                uuid = userUuid ?: "",
                received = entity.received,
                timestamp = entity.timestamp,
                ssid = entity.ssid,
                bssid = entity.bssid,
                frequency = entity.frequency,
                level = entity.level
            )
        }
        insertBatchUsingRoomEntity(roomEntities)
    }

    @Query("SELECT * FROM WifiScanEntity ORDER BY timestamp ASC")
    suspend fun getAllWifiData(): List<WifiScanEntity>

    @Query("SELECT * FROM WifiScanEntity WHERE timestamp > :afterTimestamp ORDER BY timestamp ASC")
    override suspend fun getDataAfterTimestamp(afterTimestamp: Long): List<WifiScanEntity>

    @Query("SELECT MAX(timestamp) FROM WifiScanEntity")
    override suspend fun getLatestTimestamp(): Long?

    @Query("SELECT COUNT(*) FROM WifiScanEntity")
    override suspend fun getRecordCount(): Int

    @Query("SELECT COUNT(*) FROM WifiScanEntity WHERE timestamp >= :afterTimestamp")
    suspend fun getRecordCountAfterTimestamp(afterTimestamp: Long): Int

    @Query("SELECT * FROM WifiScanEntity WHERE timestamp >= :afterTimestamp ORDER BY CASE WHEN :isAscending = 1 THEN timestamp END ASC, CASE WHEN :isAscending = 0 THEN timestamp END DESC LIMIT :limit OFFSET :offset")
    suspend fun getRecordsPaginated(afterTimestamp: Long, isAscending: Boolean, limit: Int, offset: Int): List<WifiScanEntity>

    @Query("DELETE FROM WifiScanEntity WHERE id = :recordId")
    suspend fun deleteById(recordId: Long)

    @Query("SELECT eventId FROM WifiScanEntity WHERE id = :recordId")
    suspend fun getEventIdById(recordId: Long): String?

    @Query("DELETE FROM WifiScanEntity")
    suspend fun deleteAllWifiData()

    @Query("SELECT COUNT(*) FROM WifiScanEntity WHERE timestamp >= :afterTimestamp")
    fun getDailyWifiScanCount(afterTimestamp: Long): kotlinx.coroutines.flow.Flow<Int>

    override suspend fun deleteAll() {
        deleteAllWifiData()
    }
}
