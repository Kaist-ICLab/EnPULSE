package kaist.iclab.mobiletracker.db.dao.common

/**
 * Base interface for all sensor DAOs (both phone and watch sensors).
 * Provides common operations for sensor data storage and retrieval.
 *
 * This interface is NOT annotated with @Dao — only concrete subinterfaces carry
 * that annotation, so Room/KSP only processes the leaf DAOs.
 *
 * @param TEntity The entity type for insert operations (SensorEntity for phone sensors, Room entity for watch sensors)
 * @param TRoom The Room entity type returned by queries (same as TEntity for watch sensors, different for phone sensors)
 */
interface BaseDao<TEntity, TRoom> {
    /**
     * Insert sensor data (single entity)
     * @param sensorEntity The entity to insert
     * @param userUuid The logged-in user's UUID (can be null if user not logged in)
     */
    suspend fun insert(sensorEntity: TEntity, userUuid: String? = null)

    /**
     * Insert sensor data in batch (multiple entities)
     * @param entities List of entities to insert
     * @param userUuid The logged-in user's UUID (can be null if user not logged in)
     */
    suspend fun insertBatch(entities: List<TEntity>, userUuid: String? = null)

    /**
     * Delete all data
     */
    suspend fun deleteAll()

    /**
     * Delete data older than a specific timestamp.
     * @param timestamp The timestamp threshold (in milliseconds)
     */
    suspend fun deleteDataBefore(timestamp: Long)

    /**
     * Get the latest timestamp from stored data
     */
    suspend fun getLatestTimestamp(): Long?

    /**
     * Get the total record count
     */
    suspend fun getRecordCount(): Int

    /**
     * Get data after a specific timestamp (for deduplication during upload).
     * Returns Room entities for use in upload services.
     * @param afterTimestamp The timestamp threshold (in milliseconds)
     * @return List of Room entities with timestamp greater than the threshold
     */
    suspend fun getDataAfterTimestamp(afterTimestamp: Long): List<TRoom>

    /**
     * Check if there is data after a specific timestamp (for efficient sync checks).
     * @param afterTimestamp The timestamp threshold (in milliseconds)
     * @return True if at least one record exists after the threshold
     */
    suspend fun hasDataAfterTimestamp(afterTimestamp: Long): Boolean

    /**
     * Get record count after a specific timestamp (e.g. for "today" counts).
     * @param afterTimestamp The timestamp threshold (in milliseconds)
     */
    suspend fun getRecordCountAfterTimestamp(afterTimestamp: Long): Int

    /**
     * Get paginated records with sorting support.
     * @param afterTimestamp Only include records at or after this timestamp
     * @param isAscending Sort ascending if true, descending if false
     * @param limit Maximum number of records to return
     * @param offset Number of records to skip
     */
    suspend fun getRecordsPaginated(
        afterTimestamp: Long,
        isAscending: Boolean,
        limit: Int,
        offset: Int
    ): List<TRoom>

    /**
     * Delete a specific record by its local ID.
     * @param recordId The local database ID of the record
     */
    suspend fun deleteById(recordId: Long)

    /**
     * Get the eventId for a specific record (used for remote sync deletion).
     * @param recordId The local database ID of the record
     * @return The eventId string, or null if not found
     */
    suspend fun getEventIdById(recordId: Long): String?
}
