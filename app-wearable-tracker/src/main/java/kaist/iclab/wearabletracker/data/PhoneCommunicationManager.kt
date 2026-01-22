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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
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
    private val coroutineScope: kotlinx.coroutines.CoroutineScope
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
    /**
     * Send new sensor data to the phone app via BLE (incremental sync).
     * Only sends data collected since the last successful sync.
     * Implementing Chunked Sync to avoid OOM.
     */
    fun sendDataToPhone() {
        coroutineScope.launch {
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

                // Global start time for this sync session
                val lastSyncTime = syncPreferencesHelper.getLastSyncTimestamp() ?: 0L
                var dataSent = false
                var errorOccurred = false

                // Track max timestamp seen across all sensors to update global pref at end
                var maxTimestampSeen = lastSyncTime

                // Iterate each sensor and send its data in chunks
                daos.forEach { (sensorId, dao) ->
                    // Guard to stop processing if error occurred
                    if (errorOccurred) return@forEach

                    while (coroutineContext.isActive) {
                        // Fetch a page of data
                        // We use lastSyncTime as base, because we delete sent data immediately.
                        // So getting > lastSyncTime effectively gets the "next" available data.
                        val data = dao.getDataSince(
                            lastSyncTime,
                            kaist.iclab.wearabletracker.Constants.DB.SYNC_BATCH_LIMIT
                        )

                        if (data.isEmpty()) {
                            break // Sensor done
                        }

                        dataSent = true

                        // Calculate max timestamp in this specific chunk
                        val chunkMaxTimestamp = data.maxOf { it.timestamp }
                        maxTimestampSeen = maxOf(maxTimestampSeen, chunkMaxTimestamp)

                        val batchId = UUID.randomUUID().toString()

                        // Build CSV for this chunk
                        val csvBuilder = StringBuilder()
                        csvBuilder.append("BATCH:$batchId\n")
                        csvBuilder.append("SINCE:$lastSyncTime\n")
                        csvBuilder.append("---DATA---\n")
                        csvBuilder.append("$sensorId\n")
                        csvBuilder.append(data.first().toCsvHeader() + "\n")
                        data.forEach { entity ->
                            csvBuilder.append(entity.toCsvRow() + "\n")
                        }
                        // Add newline at end of batch
                        csvBuilder.append("\n")

                        val batch = SyncBatch(
                            batchId = batchId,
                            startTimestamp = lastSyncTime,
                            endTimestamp = chunkMaxTimestamp,
                            recordCount = data.size,
                            createdAt = System.currentTimeMillis()
                        )

                        // We do NOT save pending batch persistently here to avoid IO overhead in loop,
                        // and because we treat `bleChannel.send` + `delete` as an atomic unit for this chunk.

                        try {
                            bleChannel.send(Constants.BLE.KEY_SENSOR_DATA, csvBuilder.toString())

                            // Send/Delete succeeded for this chunk
                            dao.deleteDataBefore(chunkMaxTimestamp)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error sending chunk for $sensorId: ${e.message}", e)
                            errorOccurred = true
                            break // Stop this sensor loop
                        }
                    }
                }

                if (dataSent && !errorOccurred) {
                    // Update global last sync timestamp
                    // Use System.currentTimeMillis() to be conservative for next sync, 
                    // or use maxTimestampSeen. System time is consistent with original logic.
                    syncPreferencesHelper.saveLastSyncTimestamp(System.currentTimeMillis())

                    withContext(Dispatchers.Main) {
                        NotificationHelper.showPhoneCommunicationSuccess(androidContext)
                    }
                } else if (!dataSent) {
                    Log.w(TAG, "No new data to send")
                    withContext(Dispatchers.Main) {
                        NotificationHelper.showPhoneCommunicationFailure(
                            androidContext,
                            androidContext.getString(R.string.notification_no_data)
                        )
                    }
                } else {
                    // Error case handled by previous logging/notification? 
                    withContext(Dispatchers.Main) {
                        NotificationHelper.showPhoneCommunicationFailure(
                            androidContext,
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
}
