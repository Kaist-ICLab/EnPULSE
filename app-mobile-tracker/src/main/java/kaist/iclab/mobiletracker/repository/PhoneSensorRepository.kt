package kaist.iclab.mobiletracker.repository

import kaist.iclab.tracker.sensor.core.SensorEntity

/**
 * Repository interface for phone sensor data operations.
 * Provides abstraction for phone sensor data storage and retrieval using Room database.
 */
interface PhoneSensorRepository {
    /**
     * Insert sensor data for a specific sensor
     * @param sensorId The ID of the sensor
     * @param entity The sensor entity to insert
     * @return Result indicating success or failure with error details
     */
    suspend fun insertSensorData(sensorId: String, entity: SensorEntity): Result<Unit>

    /**
     * Delete all data for a specific sensor
     * @param sensorId The ID of the sensor
     * @return Result indicating success or failure with error details
     */
    suspend fun deleteAllSensorData(sensorId: String): Result<Unit>

    /**
     * Check if a sensor has a registered data storage
     * @param sensorId The ID of the sensor
     * @return true if storage is available, false otherwise
     */
    fun hasStorageForSensor(sensorId: String): Boolean

    /**
     * Delete all sensor data from all sensors
     * @return Result indicating success or failure with error details
     */
    suspend fun flushAllData(): Result<Unit>

    /**
     * Get the latest recorded timestamp for a specific sensor
     * @param sensorId The ID of the sensor
     * @return Latest timestamp in milliseconds, or null if no data exists
     */
    suspend fun getLatestRecordedTimestamp(sensorId: String): Long?

    /**
     * Get the record count for a specific sensor
     * @param sensorId The ID of the sensor
     * @return Number of records stored for this sensor
     */
    suspend fun getRecordCount(sensorId: String): Int
}

