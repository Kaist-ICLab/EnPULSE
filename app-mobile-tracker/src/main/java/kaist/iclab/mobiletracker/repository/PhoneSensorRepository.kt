package kaist.iclab.mobiletracker.repository

import kaist.iclab.tracker.sensor.core.SensorEntity

/**
 * Repository interface for phone sensor data operations.
 * Provides abstraction for phone sensor data storage and retrieval using Room database.
 */
interface PhoneSensorRepository {
    /**
     * Insert sensor data in batch
     * @param sensorId The ID of the sensor
     * @param entities List of sensor entities to insert
     * @return Result indicating success or failure
     */
    suspend fun insertSensorDataBatch(sensorId: String, entities: List<SensorEntity>): Result<Unit>

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
     * Get the record count for a specific sensor
     * @param sensorId The ID of the sensor
     * @return Number of records stored for this sensor
     */
    suspend fun getRecordCount(sensorId: String): Int
}

