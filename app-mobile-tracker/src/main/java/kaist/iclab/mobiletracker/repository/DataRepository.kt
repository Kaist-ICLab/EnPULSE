package kaist.iclab.mobiletracker.repository

/**
 * Data class representing a sensor's summary information for the Data screen.
 */
data class SensorInfo(
    val sensorId: String,
    val displayName: String,
    val recordCount: Int,
    val lastRecordedTime: Long?,
    val isWatchSensor: Boolean = false,
    val isPhoneSensor: Boolean = false
)

/**
 * Data class representing detailed sensor information for the Sensor Detail screen.
 */
data class SensorDetailInfo(
    val sensorId: String,
    val displayName: String,
    val totalRecords: Int,
    val todayRecords: Int,
    val lastRecordedTime: Long?,
    val lastSyncTimestamp: Long? = null,
    val isWatchSensor: Boolean = false,
    val isPhoneSensor: Boolean = false
)

/**
 * Data class representing a single sensor record for display.
 */
data class SensorRecord(
    val id: Long,
    val timestamp: Long,
    val fields: Map<String, String>
)

/**
 * Enum for date filter options.
 */
enum class DateFilter {
    TODAY,
    LAST_7_DAYS,
    LAST_30_DAYS,
    ALL_TIME,
    CUSTOM
}

/**
 * Enum for page size options.
 */
enum class PageSize(val value: Int) {
    SIZE_25(25),
    SIZE_50(50),
    SIZE_100(100),
    SIZE_250(250),
    SIZE_500(500),
    SIZE_1000(1000),
}

/**
 * Enum for sort order options.
 */
enum class SortOrder {
    NEWEST_FIRST,
    OLDEST_FIRST
}

/**
 * Repository interface for the Data screen.
 * Provides access to sensor data counts and metadata.
 */
interface DataRepository {
    /**
     * Get summary information for all sensors.
     * @return List of SensorInfo for all phone and watch sensors
     */
    suspend fun getAllSensorInfo(): List<SensorInfo>
    
    /**
     * Get summary information for a specific sensor.
     * @param sensorId The sensor ID
     * @return SensorInfo or null if not found
     */
    suspend fun getSensorInfo(sensorId: String): SensorInfo?
    
    /**
     * Get detailed information for a specific sensor.
     * @param sensorId The sensor ID
     * @return SensorDetailInfo with counts and timestamps
     */
    suspend fun getSensorDetailInfo(sensorId: String): SensorDetailInfo?
    
    /**
     * Get paginated sensor records.
     * @param sensorId The sensor ID
     * @param dateFilter Date filter option
     * @param sortOrder Sort order
     * @param limit Number of records per page
     * @param offset Offset for pagination
     * @return List of SensorRecord
     */
    suspend fun getSensorRecords(
        sensorId: String,
        dateFilter: DateFilter,
        sortOrder: SortOrder,
        limit: Int,
        offset: Int
    ): List<SensorRecord>
    
    /**
     * Get total count of records for a sensor with date filter.
     */
    suspend fun getSensorRecordCount(sensorId: String, dateFilter: DateFilter): Int
    
    /**
     * Delete a specific record.
     * @param sensorId The sensor ID
     * @param recordId The record ID to delete
     */
    suspend fun deleteRecord(sensorId: String, recordId: Long)
    
    /**
     * Delete all data for a specific sensor.
     * @param sensorId The sensor ID
     */
    suspend fun deleteAllSensorData(sensorId: String)
    
    /**
     * Upload sensor data to server.
     * @param sensorId The sensor ID
     * @return Number of records uploaded, or -1 on failure
     */
    suspend fun uploadSensorData(sensorId: String): Int
    
    /**
     * Get the last sync timestamp for a sensor.
     * @param sensorId The sensor ID
     * @return Timestamp in milliseconds, or null if never synced
     */
    suspend fun getLastSyncTimestamp(sensorId: String): Long?

    /**
     * Upload all data for all sensors.
     * @return Number of sensors that had data to upload
     */
    suspend fun uploadAllData(): Int

    /**
     * Delete all data for all sensors.
     */
    suspend fun deleteAllAllData()
    
    /**
     * Get all records for a sensor (for export, no pagination).
     * @param sensorId The sensor ID
     * @param dateFilter Date filter option
     * @return List of all SensorRecord matching the filter
     */
    suspend fun getAllSensorRecordsForExport(sensorId: String, dateFilter: DateFilter): List<SensorRecord>
}
