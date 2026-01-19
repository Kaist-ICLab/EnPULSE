package kaist.iclab.mobiletracker.repository.handlers

import kaist.iclab.mobiletracker.repository.SensorRecord

/**
 * Interface for handling sensor-specific data operations.
 * Each sensor type has its own handler implementation that encapsulates
 * DAO operations and entity-to-record mapping.
 */
interface SensorDataHandler {
    /** Unique identifier for the sensor (e.g., "Location", "Battery") */
    val sensorId: String
    
    /** Human-readable display name */
    val displayName: String
    
    /** Whether this is a watch sensor (vs phone sensor) */
    val isWatchSensor: Boolean

    /** Whether this sensor has phone-collected data (default: opposite of watch sensor) */
    val isPhoneSensor: Boolean get() = !isWatchSensor || sensorId == "Location"

    /** Get total record count for this sensor */
    suspend fun getRecordCount(): Int
    
    /** Get the timestamp of the most recent record */
    suspend fun getLatestTimestamp(): Long?
    
    /** Get record count after a specific timestamp */
    suspend fun getRecordCountAfterTimestamp(timestamp: Long): Int
    
    /** Get paginated records with filtering and sorting */
    suspend fun getRecordsPaginated(
        afterTimestamp: Long,
        isAscending: Boolean,
        limit: Int,
        offset: Int
    ): List<SensorRecord>
    
    /** Delete all records for this sensor */
    suspend fun deleteAll()
    
    /** Delete a specific record by ID */
    suspend fun deleteById(id: Long)
    
    /** Get the eventId for a specific record by ID (for remote sync) */
    suspend fun getEventIdById(id: Long): String?
    
    /** Get the Supabase table name for this sensor */
    val supabaseTableName: String
}
