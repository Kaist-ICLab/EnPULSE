package kaist.iclab.tracker.sync.ble

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kaist.iclab.tracker.R
import kaist.iclab.tracker.sync.core.DataChannelSender
import kotlinx.coroutines.tasks.await

/**
 * BLE data sender for sending data through Bluetooth Low Energy.
 * Uses Google Wearable DataLayer API for communication.
 * 
 * Internal class - only accessible through BLEDataChannel.
 */
internal class BLESender(
    private val context: Context
) : DataChannelSender<Unit>() {
    companion object {
        private val TAG = BLESender::class.simpleName
    }

    private val dataClient by lazy { Wearable.getDataClient(context) }

    override suspend fun send(key: String, value: String) {
        send(key, value, false)
    }

    suspend fun send(key: String, value: String, isUrgent: Boolean) {
        val basePath = context.getString(R.string.ble_sync_path)
        // Append timestamp and key to make each message unique
        // This ensures that multiple messages with the same key are treated as separate data items
        val uniquePath = "$basePath/$key/${System.currentTimeMillis()}-${System.nanoTime()}"
        val asset = Asset.createFromBytes(value.toByteArray())

        Log.d(TAG, "send: $key, $value (path: $uniquePath)")

        val request = PutDataMapRequest.create(uniquePath).apply {
            dataMap.putString("key", key)
            dataMap.putAsset("data", asset)
        }
            .asPutDataRequest()

        val dataItem = if (isUrgent) request.setUrgent() else request
        val result = dataClient.putDataItem(dataItem).await()
    }
}
