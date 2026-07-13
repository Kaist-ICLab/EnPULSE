package kaist.iclab.wearabletracker.repository

import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for watch sensor data operations.
 * Abstracts data access from the ViewModel layer.
 */
interface WatchSensorRepository {
    /**
     * Delete all sensor data from local storage.
     */
    suspend fun deleteAllSensorData(): Result<Unit>

    /**
     * Get the last sync timestamp.
     * @return timestamp in milliseconds, or null if never synced
     */
    fun getLastSyncTimestamp(): Long?

    /**
     * Flow of the last sync timestamp.
     */
    val lastSyncTimestampFlow: Flow<Long?>

    /**
     * Get the total number of records across all sensors since the given timestamp.
     */
    suspend fun getRecordCountSince(timestamp: Long): Int

    /**
     * Get the total number of records across all sensors.
     */
    suspend fun getTotalRecordCount(): Int

    // Auto-sync settings
    val autoSyncEnabledFlow: Flow<Boolean>
    val autoSyncIntervalFlow: Flow<Long>
    fun setAutoSyncEnabled(enabled: Boolean)
    fun setAutoSyncInterval(intervalMs: Long)
}
