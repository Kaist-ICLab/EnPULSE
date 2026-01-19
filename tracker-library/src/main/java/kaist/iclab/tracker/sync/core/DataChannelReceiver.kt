package kaist.iclab.tracker.sync.core

import kotlinx.serialization.json.JsonElement

/**
 * Abstract base class for data receivers.
 * Provides common functionality for receiving data through various channels.
 */
abstract class DataChannelReceiver : DataReceiver {
    /**
     * Map of keys to their registered callback lists
     */
    protected val callbackList = mutableMapOf<String, MutableList<(String, JsonElement) -> Unit>>()

    /**
     * Add a listener for specific keys
     */
    override fun addOnReceivedListener(keys: Set<String>, callback: (String, JsonElement) -> Unit) {
        for (key in keys) {
            callbackList.getOrPut(key) { mutableListOf() }.add(callback)
        }
    }

    /**
     * Remove a listener for specific keys
     */
    override fun removeOnReceivedListener(
        keys: Set<String>,
        callback: (String, JsonElement) -> Unit
    ) {
        for (key in keys) {
            callbackList[key]?.remove(callback)
        }
    }

    /**
     * Notify all registered callbacks for a specific key
     */
    protected fun notifyCallbacks(key: String, value: JsonElement) {
        callbackList[key]?.forEach { callback ->
            callback(key, value)
        }
    }
}
