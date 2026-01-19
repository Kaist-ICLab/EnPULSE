package kaist.iclab.wearabletracker.data

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import kaist.iclab.tracker.sync.ble.BLEDataChannel
import kaist.iclab.wearabletracker.Constants
import kaist.iclab.wearabletracker.R
import kaist.iclab.wearabletracker.db.dao.BaseDao
import kaist.iclab.wearabletracker.helpers.NotificationHelper
import kaist.iclab.wearabletracker.helpers.SyncPreferencesHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class PhoneCommunicationManager(
    private val androidContext: Context,
    private val daos: Map<String, BaseDao<*>>,
    private val syncPreferencesHelper: SyncPreferencesHelper,
) {
    private val TAG = javaClass.simpleName
    private val bleChannel: BLEDataChannel = BLEDataChannel(androidContext)
    private val nodeClient: NodeClient by lazy { Wearable.getNodeClient(androidContext) }

    fun getBleChannel(): BLEDataChannel = bleChannel

    /**
     * Check if phone node is available and reachable
     */
    private suspend fun isPhoneAvailable(): Boolean = try {
        val connectedNodes = suspendCancellableCoroutine<List<Node>> { continuation ->
            nodeClient.connectedNodes
                .addOnSuccessListener { continuation.resume(it) }
                .addOnFailureListener { continuation.resumeWithException(it) }
        }
        connectedNodes.isNotEmpty()
    } catch (e: Exception) {
        Log.e(TAG, "Error checking phone availability: ${e.message}", e)
        false
    }

    /**
     * Send new sensor data to the phone app via BLE (incremental sync).
     * Only sends data collected since the last successful sync.
     */
    fun sendDataToPhone() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!isPhoneAvailable()) {
                    Log.e(
                        TAG,
                        "Error sending data to phone: Phone is not available or not connected"
                    )
                    withContext(Dispatchers.Main) {
                        NotificationHelper.showPhoneCommunicationFailure(
                            androidContext,
                            androidContext.getString(R.string.notification_phone_not_available)
                        )
                    }
                    return@launch
                }

                val result = generateIncrementalCSVData()
                if (result == null) {
                    Log.w(TAG, "No new data to send")
                    withContext(Dispatchers.Main) {
                        NotificationHelper.showPhoneCommunicationFailure(
                            androidContext,
                            androidContext.getString(R.string.notification_no_data)
                        )
                    }
                    return@launch
                }

                val (batch, csvData) = result

                // Save pending batch BEFORE sending (for recovery if interrupted)
                syncPreferencesHelper.savePendingBatch(batch)

                try {
                    bleChannel.send(Constants.BLE.KEY_SENSOR_DATA, csvData)

                    // Immediate confirmation (fallback). Reliable cleanup is handled by SyncAckListener.
                    onSyncConfirmed(batch)

                    withContext(Dispatchers.Main) {
                        NotificationHelper.showPhoneCommunicationSuccess(androidContext)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending data to phone: ${e.message}", e)
                    // Keep pending batch for retry - don't clear it
                    withContext(Dispatchers.Main) {
                        NotificationHelper.showPhoneCommunicationFailure(
                            androidContext,
                            e,
                            androidContext.getString(R.string.notification_send_failed)
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in sendDataToPhone: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    NotificationHelper.showPhoneCommunicationFailure(
                        androidContext,
                        e,
                        "Error in sendDataToPhone"
                    )
                }
            }
        }
    }

    /**
     * Called when sync is confirmed (either by ACK or immediately for now).
     * Updates last sync timestamp and deletes synced data from watch.
     */
    private suspend fun onSyncConfirmed(batch: SyncBatch) {
        // Delete synced data from all DAOs
        daos.values.forEach { dao ->
            dao.deleteDataBefore(batch.endTimestamp)
        }

        // Update last sync timestamp
        syncPreferencesHelper.saveLastSyncTimestamp(batch.endTimestamp)

        // Clear pending batch
        syncPreferencesHelper.clearPendingBatch()
    }

    /**
     * Generate CSV data for incremental sync.
     * Only includes data since the last successful sync.
     * Returns null if there's no new data to send.
     */
    private suspend fun generateIncrementalCSVData(): Pair<SyncBatch, String>? {
        val lastSyncTime = syncPreferencesHelper.getLastSyncTimestamp() ?: 0L
        val batchId = UUID.randomUUID().toString()
        val csvBuilder = StringBuilder()
        var totalRecords = 0
        var maxTimestamp = lastSyncTime

        // Add batch header
        csvBuilder.append("BATCH:$batchId\n")
        csvBuilder.append("SINCE:$lastSyncTime\n")
        csvBuilder.append("---DATA---\n")

        daos.forEach { (sensorId, dao) ->
            val data = dao.getDataSince(lastSyncTime)
            if (data.isNotEmpty()) {
                csvBuilder.append("$sensorId\n")
                csvBuilder.append(data.first().toCsvHeader() + "\n")
                data.forEach { entity ->
                    csvBuilder.append(entity.toCsvRow() + "\n")
                    // We need to track max timestamp - parse from row or use system time
                }
                totalRecords += data.size
                csvBuilder.append("\n")
            }
        }

        // No new data
        if (totalRecords == 0) {
            return null
        }

        // Use current time as end timestamp (conservative - ensures no data loss)
        maxTimestamp = System.currentTimeMillis()

        val batch = SyncBatch(
            batchId = batchId,
            startTimestamp = lastSyncTime,
            endTimestamp = maxTimestamp,
            recordCount = totalRecords,
            createdAt = System.currentTimeMillis()
        )

        return Pair(batch, csvBuilder.toString())
    }
}
