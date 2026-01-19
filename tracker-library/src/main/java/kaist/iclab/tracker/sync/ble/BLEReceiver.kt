package kaist.iclab.tracker.sync.ble

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kaist.iclab.tracker.sync.core.DataChannelReceiver
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

/**
 * BLE data receiver for receiving data through Bluetooth Low Energy.
 * Uses Google Wearable DataLayer API for communication.
 * 
 * Internal class - only accessible through BLEDataChannel.
 */
internal class BLEReceiver : DataChannelReceiver() {
    companion object {
        private val TAG = BLEReceiver::class.simpleName
        private var localNodeId: String? = null

        // Synchronized callback list that's shared between activity and service
        @Volatile
        private var sharedCallbackList: MutableMap<String, MutableList<(String, JsonElement) -> Unit>>? =
            null

        fun getSharedCallbackList(): MutableMap<String, MutableList<(String, JsonElement) -> Unit>> {
            return sharedCallbackList ?: synchronized(this) {
                sharedCallbackList
                    ?: mutableMapOf<String, MutableList<(String, JsonElement) -> Unit>>().also {
                        sharedCallbackList = it
                    }
            }
        }
    }

    init {
        // Use the shared callback list
        val sharedList = getSharedCallbackList()
        synchronized(sharedList) {
            callbackList.clear()
            callbackList.putAll(sharedList)
        }
        BLEReceiverService.callbackList = getSharedCallbackList()
    }

    fun initializeLocalNodeId(context: Context) {
        Wearable.getNodeClient(context).localNode.addOnSuccessListener { node ->
            localNodeId = node.id
        }
    }

    override fun addOnReceivedListener(keys: Set<String>, callback: (String, JsonElement) -> Unit) {
        super.addOnReceivedListener(keys, callback)
        // Also add to shared callback list
        val sharedList = getSharedCallbackList()
        synchronized(sharedList) {
            for (key in keys) {
                sharedList.getOrPut(key) { mutableListOf() }.add(callback)
            }
        }
        Log.d(TAG, "Added listeners for keys: $keys")
    }

    override fun removeOnReceivedListener(
        keys: Set<String>,
        callback: (String, JsonElement) -> Unit
    ) {
        super.removeOnReceivedListener(keys, callback)
        // Also remove from shared callback list
        val sharedList = getSharedCallbackList()
        synchronized(sharedList) {
            for (key in keys) {
                sharedList[key]?.remove(callback)
            }
        }
        Log.d(TAG, "Removed listeners for keys: $keys")
    }

    class BLEReceiverService : WearableListenerService() {
        companion object {
            private val TAG = BLEReceiverService::class.simpleName
            var callbackList = mutableMapOf<String, MutableList<(String, JsonElement) -> Unit>>()
        }

        override fun onCreate() {
            super.onCreate()
            initializeLocalNodeId()
            // Use the shared callback list
            callbackList = getSharedCallbackList()
        }

        private fun initializeLocalNodeId() {
            Wearable.getNodeClient(this).localNode.addOnSuccessListener { node ->
                BLEReceiver.localNodeId = node.id
            }
        }

        private fun onAssetSuccessListener(
            callbacks: List<(String, JsonElement) -> Unit>,
            key: String,
            assetFd: DataClient.GetFdForAssetResponse
        ) {
            assetFd.inputStream.use { inputStream ->
                val jsonString = String(inputStream.readBytes())
                Log.d(TAG, "Received data for key '$key': $jsonString")
                
                // Try to parse as JSON, if it fails, wrap it as a JSON string
                val jsonElement = try {
                    Json.parseToJsonElement(jsonString)
                } catch (e: Exception) {
                    // If it's not valid JSON, wrap it properly as a JsonPrimitive string
                    JsonPrimitive(jsonString)
                }

                callbacks.forEach { it.invoke(key, jsonElement) }
            }
        }

        override fun onDataChanged(dataEvents: DataEventBuffer) {
            // Always get fresh callback list to ensure we have latest registered listeners
            // This fixes the race condition where service starts before listeners are registered
            val currentCallbackList = getSharedCallbackList()
            val currentNodeId = BLEReceiver.localNodeId
            
            dataEvents.forEach { dataEvent ->
                // Skip if this is a message from the same device
                if (currentNodeId != null && dataEvent.dataItem.uri.host == currentNodeId) {
                    return@forEach
                }

                val dataMapItem = DataMapItem.fromDataItem(dataEvent.dataItem)
                val key = dataMapItem.dataMap.getString("key")

                if (key == null) {
                    return@forEach
                }

                // Read from shared list (always up-to-date) instead of cached callbackList
                val callbacks = synchronized(currentCallbackList) {
                    currentCallbackList[key]?.toList() ?: listOf()
                }
                
                if (callbacks.isEmpty()) {
                    Log.w(TAG, "No callbacks registered for key: $key")
                    return@forEach
                }

                val asset = DataMapItem.fromDataItem(dataEvent.dataItem).dataMap.getAsset("data")
                if (asset != null) {
                    Wearable.getDataClient(this).getFdForAsset(asset)
                        .addOnSuccessListener {
                            onAssetSuccessListener(callbacks, key, it)
                        }
                        .addOnFailureListener { exception ->
                            Log.e(TAG, "Failed to get asset for key '$key': ${exception.message}")
                        }
                }
            }
        }
    }
}
