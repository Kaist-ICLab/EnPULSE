package kaist.iclab.mobiletracker.repository

import kaist.iclab.mobiletracker.db.entity.common.LocationEntity
import kaist.iclab.mobiletracker.db.entity.watch.WatchAccelerometerEntity
import kaist.iclab.mobiletracker.db.entity.watch.WatchEDAEntity
import kaist.iclab.mobiletracker.db.entity.watch.WatchHeartRateEntity
import kaist.iclab.mobiletracker.db.entity.watch.WatchPPGEntity
import kaist.iclab.mobiletracker.db.entity.watch.WatchSkinTemperatureEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for watch sensor data storage operations.
 * Provides methods for storing and retrieving watch sensor data from Room database.
 * 
 * Note: Watch sensors insert data in batches (List<Entity>) unlike phone sensors which insert
 * single entities. This is due to the nature of watch data being received in CSV batches.
 */
interface WatchSensorRepository {
    /**
     * Store heart rate sensor data
     */
    suspend fun insertHeartRateData(entities: List<WatchHeartRateEntity>): Result<Unit>
    
    /**
     * Store accelerometer sensor data
     */
    suspend fun insertAccelerometerData(entities: List<WatchAccelerometerEntity>): Result<Unit>
    
    /**
     * Store EDA sensor data
     */
    suspend fun insertEDAData(entities: List<WatchEDAEntity>): Result<Unit>
    
    /**
     * Store PPG sensor data
     */
    suspend fun insertPPGData(entities: List<WatchPPGEntity>): Result<Unit>
    
    /**
     * Store skin temperature sensor data
     */
    suspend fun insertSkinTemperatureData(entities: List<WatchSkinTemperatureEntity>): Result<Unit>
    
    /**
     * Store location sensor data
     */
    suspend fun insertLocationData(entities: List<LocationEntity>): Result<Unit>
    
    /**
     * Get latest timestamp for a watch sensor
     * @param sensorId The sensor ID (e.g., "Heart Rate", "Acceleration", etc.)
     * @return Latest timestamp or null if no data exists
     */
    suspend fun getLatestTimestamp(sensorId: String): Long?
    
    /**
     * Get record count for a watch sensor
     * @param sensorId The sensor ID
     * @return Number of records stored
     */
    suspend fun getRecordCount(sensorId: String): Int
    
    /**
     * Delete all data for a specific watch sensor
     * @param sensorId The sensor ID
     * @return Result indicating success or failure
     */
    suspend fun deleteAllSensorData(sensorId: String): Result<Unit>
    
    /**
     * Delete all watch sensor data from all sensors
     * @return Result indicating success or failure
     */
    suspend fun flushAllData(): Result<Unit>

    /**
     * Observes the real-time connection info including status and connected device names
     */
    fun getWatchConnectionInfo(): Flow<WatchConnectionInfo>
}

