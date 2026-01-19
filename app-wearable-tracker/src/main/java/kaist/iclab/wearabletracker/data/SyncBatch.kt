package kaist.iclab.wearabletracker.data

/**
 * Represents a sync batch for tracking incremental sync operations.
 * Used to confirm receipt and enable recovery if sync is interrupted.
 */
data class SyncBatch(
    /** Unique identifier for this sync batch (UUID) */
    val batchId: String,

    /** Timestamp of earliest data point in this batch */
    val startTimestamp: Long,

    /** Timestamp of latest data point in this batch */
    val endTimestamp: Long,

    /** Total number of records in this batch (across all sensors) */
    val recordCount: Int,

    /** When this batch was created (system time) */
    val createdAt: Long
)
