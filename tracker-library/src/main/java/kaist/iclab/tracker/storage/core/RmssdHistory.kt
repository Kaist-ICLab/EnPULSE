package kaist.iclab.tracker.storage.core

/**
 * Persistent store of RMSSD samples used by StressSensor to compute the
 * percentile threshold without keeping the full history in memory.
 */
interface RmssdHistory {
    suspend fun insert(timestampMs: Long, rmssd: Float)
    suspend fun all(): FloatArray
    suspend fun clear()
}
