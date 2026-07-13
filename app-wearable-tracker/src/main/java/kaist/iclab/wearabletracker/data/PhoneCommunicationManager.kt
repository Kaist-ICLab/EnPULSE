package kaist.iclab.wearabletracker.data

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import kaist.iclab.tracker.sync.ble.BLEDataChannel
import kaist.iclab.wearabletracker.Constants
import kaist.iclab.wearabletracker.R
import kaist.iclab.wearabletracker.db.entity.CsvSerializable
import kaist.iclab.wearabletracker.db.obx.WatchSensorStore
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
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class PhoneCommunicationManager(
    private val androidContext: Context,
    private val stores: Map<String, WatchSensorStore<*>>,
    private val syncPreferencesHelper: SyncPreferencesHelper,
    private val coroutineScope: kotlinx.coroutines.CoroutineScope
) {
    private val TAG = javaClass.simpleName
    private val bleChannel: BLEDataChannel = BLEDataChannel(androidContext)
    private val nodeClient: NodeClient by lazy { Wearable.getNodeClient(androidContext) }
    private val syncMutex = Mutex()

    private val _syncProgress = MutableStateFlow<Float?>(null)
    val syncProgress: StateFlow<Float?> = _syncProgress.asStateFlow()

    fun getBleChannel(): BLEDataChannel = bleChannel

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

    fun sendDataToPhone(isSilent: Boolean = false) {
        coroutineScope.launch {
            if (!syncMutex.tryLock()) {
                Log.w(TAG, "Sync already in progress, skipping")
                return@launch
            }
            try {
                val result = ErrorClassifier.runClassified(TAG, "send data to phone") {
                    val totalRecordsToSync = stores.entries.sumOf { (sensorId, store) ->
                        store.getCountSince(syncPreferencesHelper.getSensorWatermark(sensorId) ?: 0L)
                    }
                    if (totalRecordsToSync == 0) {
                        return@runClassified false
                    }

                    _syncProgress.value = 0f
                    var dataSent = false
                    var totalRecordsSentSoFar = 0

                    stores.forEach { (sensorId, store) ->
                        var currentSensorLastTimestamp = syncPreferencesHelper.getSensorWatermark(sensorId) ?: 0L

                        while (coroutineContext.isActive) {
                            val data = store.getDataSince(
                                currentSensorLastTimestamp,
                                Constants.DB.SYNC_BATCH_LIMIT
                            )

                            if (data.isEmpty()) break

                            dataSent = true
                            totalRecordsSentSoFar += data.size
                            _syncProgress.value = totalRecordsSentSoFar.toFloat() / totalRecordsToSync

                            val chunkStartTs = currentSensorLastTimestamp
                            val chunkMaxTimestamp = data.maxOf { it.timestamp }
                            currentSensorLastTimestamp = chunkMaxTimestamp

                            val csvBuilder = StringBuilder()
                            csvBuilder.append("SENSOR:$sensorId\n")
                            csvBuilder.append("CHUNK_START_TS:$chunkStartTs\n")
                            csvBuilder.append("CHUNK_END_TS:$chunkMaxTimestamp\n")
                            csvBuilder.append("---DATA---\n")
                            csvBuilder.append("$sensorId\n")
                            csvBuilder.append(data.first().csvHeader() + "\n")
                            data.forEach { entity -> csvBuilder.append(entity.toCsvRow() + "\n") }
                            csvBuilder.append("\n")

                            try {
                                bleChannel.send(
                                    Constants.BLE.KEY_SENSOR_DATA,
                                    csvBuilder.toString(),
                                    isUrgent = true
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "[$sensorId] Error sending chunk: ${e.message}", e)
                                throw e
                            }
                        }
                    }

                    dataSent
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
