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

    /** Human-readable name, shown while this sensor is being uploaded. */
    val displayName: String

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
     * Upload sensor data to Supabase.
     * @param userUuid The UUID of the current user
     * @param lastUploadCursor Opaque upload-progress cursor from the last successful upload; see
     * [hasDataToUpload].
     * @param allowQuarantine When a batch fails because the server explicitly rejected it (not a
     * network/auth/timeout hiccup — see [kaist.iclab.mobiletracker.repository.AppError.ServerRejected]),
     * whether to give up on that whole batch instead of failing the call: the batch is skipped via
     * [onBatchQuarantined] and never retried, and every batch after it still uploads normally, so a
     * batch's worth of bad data can't block everything captured after it forever. Callers should
     * only set this once the same spot has already failed repeatedly across separate calls — see
     * [SensorUploadService]'s streak tracking — so a single transient-looking rejection isn't
     * quarantined too eagerly.
     * @param onBatchUploaded Invoked with the updated cursor after each batch Supabase accepts,
     * before the next batch is attempted, so a run that fails part-way keeps the batches that did
     * land. Implementations that upload in more than one request must call it; the caller uses it
     * to persist upload progress incrementally.
     * @param onBatchQuarantined Invoked when [allowQuarantine] gives up on a whole batch: the
     * cursor to advance past it, how many records it contained, and why the server rejected it.
     * Those records stay in local storage; they're simply never uploaded.
     * @return Result containing the new cursor value to persist on success
     */
    suspend fun uploadData(
        userUuid: String,
        lastUploadCursor: Long,
        allowQuarantine: Boolean = false,
        onBatchUploaded: suspend (cursor: Long) -> Unit = {},
        onBatchQuarantined: suspend (cursor: Long, recordCount: Int, reason: String) -> Unit = { _, _, _ -> }
    ): Result<Long>

    /**
     * How many batches [uploadData] would send for the backlog after [lastUploadCursor] — the unit
     * the upload progress bar counts in, since one batch is roughly one unit of work while one
     * sensor can be anything from nothing to hours of it. 0 when there is nothing to upload.
     */
    suspend fun pendingBatchCount(lastUploadCursor: Long): Int

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
