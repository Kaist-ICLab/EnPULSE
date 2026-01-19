package kaist.iclab.tracker.sync.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Abstract base class for data senders.
 * Provides common functionality for sending data through various channels.
 *
 * @param R The return type of the send operation
 */
abstract class DataChannelSender<R> : DataSender<R> {
    /**
     * Send serializable data with a specific key
     */
    suspend inline fun <reified T : @Serializable Any> send(key: String, value: T): R {
        return send(key, Json.encodeToString(value))
    }

    /**
     * Abstract method to be implemented by concrete senders
     */
    abstract override suspend fun send(key: String, value: String): R
}
