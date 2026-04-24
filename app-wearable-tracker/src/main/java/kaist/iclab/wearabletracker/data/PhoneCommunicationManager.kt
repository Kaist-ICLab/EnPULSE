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
import kaist.iclab.wearabletracker.repository.ErrorClassifier
import kaist.iclab.wearabletracker.repository.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
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
    private val syncMutex = Mutex()

    // Sync progress: 0.0 to 1.0, null if not syncing
    private val _syncProgress = MutableStateFlow<Float?>(null)
    val syncProgress: StateFlow<Float?> = _syncProgress.asStateFlow()

    fun getBleChannel(): BLEDataChannel = bleChannel

    /**
     * Check if phone node is available and reachable
     */
    suspend fun isPhoneAvailable(): Boolean = try {
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
     * Implements chunked sync to avoid OOM.
     *
     * @param isSilent If true, suppresses the "Data Sent" success notification and minor UI toasts
     */
    fun sendDataToPhone(isSilent: Boolean = false) {
        coroutineScope.launch {
            if (!syncMutex.tryLock()) {
                Log.w(TAG, "Sync already in progress, skipping")
                return@launch
            }
            try {
                // Clear any stale pending batch before starting a new sync
                syncPreferencesHelper.clearStaleBatchIfNeeded()

                val result = ErrorClassifier.runClassified(TAG, "send data to phone") {
                    // We remove `isPhoneAvailable()` check here because wear OS disables proximity 
                    // scans during Doze mode sleep. We rely entirely on Play Services to eagerly
                    // queue and transmit the payload urgently when the radio wakes up.

                    // Global start time for this sync session
                    val lastSyncTime = syncPreferencesHelper.getLastSyncTimestamp() ?: 0L

                    // Calculate total records to be synced across all sensors
                    val totalRecordsToSync = daos.values.sumOf { it.getCountSince(lastSyncTime) }
                    if (totalRecordsToSync == 0) {
                        return@runClassified false
                    }

                    _syncProgress.value = 0f
                    var dataSent = false

                    // Track max timestamp seen across all sensors to update global pref at end
                    var maxTimestampSeen = lastSyncTime
                    var totalRecordsSentSoFar = 0
                    val batchId = UUID.randomUUID().toString()

                    // Per-sensor tracking
                    var successSensorCount = 0
                    var failedSensorId: String? = null

                    // Iterate each sensor and send its data in chunks
                    daos.forEach { (sensorId, dao) ->
                        var currentSensorLastTimestamp = lastSyncTime

                        while (coroutineContext.isActive) {
                            // Fetch a page of data
                            val data = dao.getDataSince(
                                currentSensorLastTimestamp,
                                Constants.DB.SYNC_BATCH_LIMIT
                            )

                            if (data.isEmpty()) {
                                break // Sensor done
                            }

                            dataSent = true
                            totalRecordsSentSoFar += data.size
                            _syncProgress.value =
                                totalRecordsSentSoFar.toFloat() / totalRecordsToSync

                            // Calculate max timestamp in this chunk
                            val chunkMaxTimestamp = data.maxOf { it.timestamp }
                            maxTimestampSeen = maxOf(maxTimestampSeen, chunkMaxTimestamp)
                            currentSensorLastTimestamp = chunkMaxTimestamp

                            // Build CSV for this chunk
                            val csvBuilder = StringBuilder()
                            csvBuilder.append("BATCH:$batchId\n")
                            csvBuilder.append("SINCE:$lastSyncTime\n")
                            csvBuilder.append("CHUNK_END_TS:$chunkMaxTimestamp\n")
                            csvBuilder.append("---DATA---\n")
                            csvBuilder.append("$sensorId\n")
                            csvBuilder.append(data.first().toCsvHeader() + "\n")
                            data.forEach { entity ->
                                csvBuilder.append(entity.toCsvRow() + "\n")
                            }
                            csvBuilder.append("\n")

                            try {
                                bleChannel.send(
                                    Constants.BLE.KEY_SENSOR_DATA,
                                    csvBuilder.toString(),
                                    isUrgent = true // Force radio wakeup during Doze deep sleep
                                )
//                                Log.d(
//                                    TAG,
//                                    "[$sensorId] Sent chunk: ${data.size} records, maxTs=$chunkMaxTimestamp"
//                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "[$sensorId] Error sending chunk: ${e.message}", e)
                                failedSensorId = sensorId
                                throw e
                            }
                        }
                        successSensorCount++
                    }

                    if (dataSent) {
//                        Log.i(
//                            TAG,
//                            "Sync payload sent: $successSensorCount sensors, batchId=$batchId"
//                        )

                        // Save as pending batch - do NOT delete yet.
                        // SyncAckListener will perform cleanup after phone confirms receipt.
                        syncPreferencesHelper.savePendingBatch(
                            SyncBatch(
                                batchId = batchId,
                                startTimestamp = lastSyncTime,
                                endTimestamp = maxTimestampSeen,
                                recordCount = totalRecordsSentSoFar,
                                createdAt = System.currentTimeMillis()
                            )
                        )
                        true
                    } else {
                        false
                    }
                }

                if (!isSilent || result is Result.Error) {
                    withContext(Dispatchers.Main) {
                        when (result) {
                            is Result.Success -> {
                                if (result.data) {
                                    NotificationHelper.showPhoneCommunicationSuccess(androidContext)
                                } else {
                                    NotificationHelper.showPhoneCommunicationFailure(
                                        androidContext,
                                        androidContext.getString(R.string.notification_no_data)
                                    )
                                }
                            }

                            is Result.Error -> {
                                NotificationHelper.showPhoneCommunicationFailure(
                                    androidContext,
                                    result.exception,
                                    "Sync failed"
                                )
                            }
                        }
                    }
                }
            } finally {
                _syncProgress.value = null
                syncMutex.unlock()
            }
        }
    }
}
