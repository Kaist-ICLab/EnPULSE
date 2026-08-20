package kaist.iclab.mobiletracker.services.upload.handlers

import kaist.iclab.mobiletracker.repository.Result

/**
 * Interface for handling sensor-specific upload operations.
 * Each sensor type has its own handler implementation that encapsulates
 * DAO, mapper, and service references for uploading data to Supabase.
 */
interface SensorUploadHandler {
    /** Unique identifier for the sensor (e.g., "Location", "Battery") */
    val sensorId: String

    /**
     * Check if there is data available to upload.
     * @param lastUploadCursor Opaque upload-progress cursor from the last successful upload (its
     * meaning is defined by the implementation — e.g. [SensorUploadHandlerImpl] uses the local
     * ObjectBox row id rather than the record's own timestamp, so out-of-order/backfilled data
     * isn't mistaken for already-uploaded; see its doc comment).
     * @return true if there is new data to upload
     */
    suspend fun hasDataToUpload(lastUploadCursor: Long): Boolean

    /**
     * Upload sensor data to Supabase, in batches.
     * @param userUuid The UUID of the current user
     * @param lastUploadCursor Opaque upload-progress cursor from the last successful upload; see
     * [hasDataToUpload].
     * @param onBatchUploaded Invoked with the new cursor value after each individual batch
     * upserts successfully — the caller should persist it immediately (e.g. [SensorUploadHandlerImpl]
     * calls it after every batch, not just once at the end). This way, if a later batch fails
     * (bad data, network drop, etc.), the batches that already succeeded aren't silently forgotten
     * and re-uploaded next time — only the failing point is retried.
     * @return Result containing the final cursor value on success (already persisted via
     * [onBatchUploaded] by the time this returns); on failure, whatever progress was made before
     * the failing batch has still been persisted via [onBatchUploaded].
     */
    suspend fun uploadData(
        userUuid: String,
        lastUploadCursor: Long,
        onBatchUploaded: suspend (Long) -> Unit = {}
    ): Result<Long>

    /**
     * Delete local data older than the specified timestamp.
     * @param beforeTimestamp The timestamp threshold for deletion
     */
    suspend fun pruneData(beforeTimestamp: Long)

    /**
     * Get the total record count available locally.
     */
    suspend fun getRecordCount(): Int

    /**
     * Get paginated records.
     * @param limit Maximum number of records
     * @param offset Number of records to skip
     * @return List of records (type generic to the handler)
     */
    suspend fun getRecordsPaginated(limit: Int, offset: Int): List<Any>

    /**
     * Get the CSV header for this sensor's data.
     */
    fun getCsvHeader(): String

    /**
     * Convert a record to a CSV row string.
     * @param record The record to convert
     */
    fun recordToCsvRow(record: Any): String
}
