package kaist.iclab.mobiletracker.db.dao.phone

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kaist.iclab.mobiletracker.db.dao.common.BaseDao
import kaist.iclab.mobiletracker.db.entity.phone.BluetoothScanEntity
import kaist.iclab.tracker.sensor.phone.BluetoothScanSensor
import kotlinx.coroutines.flow.Flow

@Dao
interface BluetoothScanDao: BaseDao<BluetoothScanSensor.Entity, BluetoothScanEntity> {
    override suspend fun insert(sensorEntity: BluetoothScanSensor.Entity, userUuid: String?) {
        val entity = BluetoothScanEntity(
            uuid = userUuid ?: "",
            received = sensorEntity.received,
            timestamp = sensorEntity.timestamp,
            name = sensorEntity.name,
            alias = sensorEntity.alias,
            address = sensorEntity.address,
            bondState = sensorEntity.bondState,
            connectionType = sensorEntity.connectionType,
            classType = sensorEntity.classType,
            rssi = sensorEntity.rssi,
            isLE = sensorEntity.isLE
        )
        insertUsingRoomEntity(entity)
    }

    @Insert
    suspend fun insertUsingRoomEntity(bluetoothScanEntity: BluetoothScanEntity)

    @Insert
    suspend fun insertBatchUsingRoomEntity(entities: List<BluetoothScanEntity>)

    override suspend fun insertBatch(entities: List<BluetoothScanSensor.Entity>, userUuid: String?) {
        val roomEntities = entities.map { entity ->
            BluetoothScanEntity(
                uuid = userUuid ?: "",
                received = entity.received,
                timestamp = entity.timestamp,
                name = entity.name,
                alias = entity.alias,
                address = entity.address,
                bondState = entity.bondState,
                connectionType = entity.connectionType,
                classType = entity.classType,
                rssi = entity.rssi,
                isLE = entity.isLE
            )
        }
        insertBatchUsingRoomEntity(roomEntities)
    }

    @Query("SELECT * FROM BluetoothScanEntity ORDER BY timestamp ASC")
    suspend fun getAllBluetoothScanData(): List<BluetoothScanEntity>

    @Query("SELECT * FROM BluetoothScanEntity WHERE timestamp > :afterTimestamp ORDER BY timestamp ASC")
    override suspend fun getDataAfterTimestamp(afterTimestamp: Long): List<BluetoothScanEntity>

    @Query("SELECT COUNT(*) FROM BluetoothScanEntity WHERE timestamp >= :afterTimestamp")
    fun getDailyBluetoothCount(afterTimestamp: Long): Flow<Int>

    @Query("SELECT MAX(timestamp) FROM BluetoothScanEntity")
    override suspend fun getLatestTimestamp(): Long?

    @Query("SELECT COUNT(*) FROM BluetoothScanEntity")
    override suspend fun getRecordCount(): Int

    @Query("SELECT COUNT(*) FROM BluetoothScanEntity WHERE timestamp >= :afterTimestamp")
    suspend fun getRecordCountAfterTimestamp(afterTimestamp: Long): Int

    @Query("SELECT * FROM BluetoothScanEntity WHERE timestamp >= :afterTimestamp ORDER BY CASE WHEN :isAscending = 1 THEN timestamp END ASC, CASE WHEN :isAscending = 0 THEN timestamp END DESC LIMIT :limit OFFSET :offset")
    suspend fun getRecordsPaginated(afterTimestamp: Long, isAscending: Boolean, limit: Int, offset: Int): List<BluetoothScanEntity>

    @Query("DELETE FROM BluetoothScanEntity WHERE id = :recordId")
    suspend fun deleteById(recordId: Long)

    @Query("SELECT eventId FROM BluetoothScanEntity WHERE id = :recordId")
    suspend fun getEventIdById(recordId: Long): String?

    @Query("DELETE FROM BluetoothScanEntity")
    suspend fun deleteAllBluetoothScanData()

    override suspend fun deleteAll() {
        deleteAllBluetoothScanData()
    }
}
