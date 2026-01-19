package kaist.iclab.tracker.sync.core

/**
 * Interface for sending data through various channels.
 *
 * @param R The return type of the send operation (e.g., Unit for fire-and-forget, Response for HTTP)
 */
interface DataSender<R> {
    /**
     * Send string data with a specific key
     */
    suspend fun send(key: String, value: String): R
}
