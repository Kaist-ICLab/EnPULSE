package kaist.iclab.wearabletracker.data

import android.util.Log
import kaist.iclab.tracker.sync.ble.BLEDataChannel
import kaist.iclab.wearabletracker.Constants
import kaist.iclab.wearabletracker.db.obx.WatchSensorStore
import kaist.iclab.wearabletracker.helpers.SyncPreferencesHelper
import kaist.iclab.wearabletracker.repository.ErrorClassifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive

class SyncAckListener(
    private val bleChannel: BLEDataChannel,
    private val stores: Map<String, WatchSensorStore<*>>,
    private val syncPreferencesHelper: SyncPreferencesHelper,
    private val coroutineScope: CoroutineScope
) {
    private val TAG = javaClass.simpleName

    fun startListening() {
        syncPreferencesHelper.clearStaleBatchIfNeeded()

        bleChannel.addOnReceivedListener(setOf(Constants.BLE.KEY_SYNC_ACK)) { _, jsonElement ->
            val ackData = when (jsonElement) {
                is JsonPrimitive -> jsonElement.content
                else -> jsonElement.toString().trim('"')
            }
            handleAck(ackData)
        }
    }

    private fun handleAck(ackData: String) {
        coroutineScope.launch {
            ErrorClassifier.runClassified(TAG, "handle sync ACK") {
                val parts = ackData.split(":")
                if (parts.size < 2) {
                    throw IllegalArgumentException("Invalid ACK format: $ackData")
                }

                val receivedBatchId = parts[0]
                val status = parts[1]
                val endTimestamp = if (parts.size >= 3) parts[2].toLongOrNull() else null

                val pendingBatch = syncPreferencesHelper.getPendingBatch()
                if (pendingBatch == null) {
                    Log.w(TAG, "Received ACK but no pending batch: $receivedBatchId")
                    return@runClassified
                }

                if (pendingBatch.batchId != receivedBatchId) {
                    Log.w(TAG, "ACK batch ID mismatch. Expected: ${pendingBatch.batchId}, Received: $receivedBatchId")
                    return@runClassified
                }

                when (status) {
                    "OK" -> onSyncConfirmed(pendingBatch, endTimestamp)
                    "FAIL" -> Log.e(TAG, "Received failure ACK for batch: $receivedBatchId")
                    else -> Log.e(TAG, "Received unknown status in ACK: $status")
                }
            }
        }
    }

    private suspend fun onSyncConfirmed(batch: SyncBatch, endTimestamp: Long?) {
        val pruneUntil = endTimestamp ?: batch.endTimestamp
        Log.d(TAG, "Pruning data up to: $pruneUntil (batchId=${batch.batchId})")

        stores.values.forEach { store -> store.deleteDataBefore(pruneUntil) }

        syncPreferencesHelper.saveLastSyncTimestamp(pruneUntil)

        if (endTimestamp == null || endTimestamp >= batch.endTimestamp) {
            syncPreferencesHelper.clearPendingBatch()
        }
    }

    fun cleanup() {}
}
