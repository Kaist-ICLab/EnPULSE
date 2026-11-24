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
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
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
    private val ackSignal: SyncAckSignal,
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

    /** @return the running sync, so a caller holding a wake lock can wait for it to finish. */
    fun sendDataToPhone(isSilent: Boolean = false): Job {
        return coroutineScope.launch {
            if (!syncMutex.tryLock()) {
                Log.w(TAG, "Sync already in progress, skipping")
                return@launch
            }
            try {
                val result = ErrorClassifier.runClassified(TAG, "send data to phone") {
                    val now = System.currentTimeMillis()

                    // Sensors still backing off after a FAIL ACK or an ACK timeout sit this
                    // round out (see SyncPreferencesHelper.recordSensorFailure).
                    val sensorsToSync = stores.filter { (sensorId, _) ->
                        val notBefore = syncPreferencesHelper.getSensorRetryNotBefore(sensorId)
                        if (notBefore != null && now < notBefore) {
                            Log.i(TAG, "[$sensorId] backing off, ${(notBefore - now) / 1000}s left")
                            false
                        } else {
                            true
                        }
                    }

                    // Per-sensor cursor to send from: the ACK-confirmed watermark, unless an
                    // earlier attempt already sent data past it that's still within its ACK
                    // timeout - in which case skip past that in-flight range instead of
                    // retransmitting data the phone may already have but hasn't ACKed yet.
                    val effectiveStartBySensor = sensorsToSync.keys.associateWith { sensorId ->
                        val confirmed = syncPreferencesHelper.getSensorWatermark(sensorId) ?: 0L
                        val pending = syncPreferencesHelper.getSensorPending(sensorId)
                        if (pending != null &&
                            now - pending.sentAt < Constants.AutoSync.PENDING_ACK_TIMEOUT_MS
                        ) {
                            maxOf(confirmed, pending.upToTimestamp)
                        } else {
                            confirmed
                        }
                    }

                    val totalRecordsToSync = sensorsToSync.entries.sumOf { (sensorId, store) ->
                        store.getCountSince(effectiveStartBySensor.getValue(sensorId))
                    }
                    if (totalRecordsToSync == 0) {
                        return@runClassified false
                    }

                    _syncProgress.value = 0f
                    var dataSent = false
                    var totalRecordsSentSoFar = 0

                    sensorsToSync.forEach { (sensorId, store) ->
                        val sent = sendSensorWindowed(
                            sensorId,
                            store,
                            effectiveStartBySensor.getValue(sensorId)
                        ) { rows ->
                            totalRecordsSentSoFar += rows
                            _syncProgress.value =
                                (totalRecordsSentSoFar.toFloat() / totalRecordsToSync).coerceAtMost(1f)
                        }
                        if (sent) dataSent = true
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

    /**
     * Sends one sensor's unsynced data as a sliding window: at most
     * [Constants.AutoSync.MAX_CHUNKS_IN_FLIGHT] chunks handed to the Data Layer and not yet
     * ACKed, topping the window up as ACKs advance the watermark. Gives up on this sensor - with
     * exponential backoff - after [Constants.AutoSync.PENDING_ACK_TIMEOUT_MS] without ACK
     * progress; a FAIL ACK stops it too (SyncAckListener has already backed it off).
     *
     * @return whether any chunk was sent.
     */
    private suspend fun sendSensorWindowed(
        sensorId: String,
        store: WatchSensorStore<*>,
        startTimestamp: Long,
        onRowsSent: (Int) -> Unit
    ): Boolean {
        val sensorStartedAt = System.currentTimeMillis()
        var nextFrom = startTimestamp
        // Send only the rows that exist now; rows saved during this sync wait for the next one.
        // The watch saves new rows every few seconds, so without this cut-off a collecting
        // sensor almost always has fresh rows when an ACK lands, and its turn never ends.
        val sendUpTo = store.getLatestTimestamp() ?: startTimestamp
        val inFlightChunkEnds = ArrayDeque<Long>()
        var lastProgressAt = sensorStartedAt
        var sentAny = false

        while (currentCoroutineContext().isActive) {
            // Read the version before checking state, so an ACK landing in between wakes the
            // wait below instead of being missed.
            val seenVersion = ackSignal.version

            val confirmed = syncPreferencesHelper.getSensorWatermark(sensorId) ?: 0L
            var progressed = false
            while (inFlightChunkEnds.isNotEmpty() && inFlightChunkEnds.first() <= confirmed) {
                inFlightChunkEnds.removeFirst()
                progressed = true
            }
            if (progressed) lastProgressAt = System.currentTimeMillis()

            if (ackSignal.failedSince(sensorId, sensorStartedAt)) {
                Log.w(TAG, "[$sensorId] phone returned FAIL, stopping this sensor")
                break
            }

            while (inFlightChunkEnds.size < Constants.AutoSync.MAX_CHUNKS_IN_FLIGHT &&
                nextFrom < sendUpTo
            ) {
                val data = store.getDataSince(nextFrom, Constants.DB.SYNC_BATCH_LIMIT)
                if (data.isEmpty()) break

                val chunkEnd = data.maxOf { it.timestamp }
                try {
                    bleChannel.send(
                        Constants.BLE.KEY_SENSOR_DATA,
                        buildChunk(sensorId, nextFrom, chunkEnd, data),
                        isUrgent = true
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "[$sensorId] Error sending chunk: ${e.message}", e)
                    throw e
                }
                if (inFlightChunkEnds.isEmpty()) lastProgressAt = System.currentTimeMillis()
                inFlightChunkEnds.addLast(chunkEnd)
                nextFrom = chunkEnd
                sentAny = true
                syncPreferencesHelper.setSensorPending(sensorId, chunkEnd, System.currentTimeMillis())
                onRowsSent(data.size)
            }

            if (inFlightChunkEnds.isEmpty()) break // everything sent has been ACKed

            val waitMs = Constants.AutoSync.PENDING_ACK_TIMEOUT_MS -
                    (System.currentTimeMillis() - lastProgressAt)
            if (waitMs <= 0) {
                val now = System.currentTimeMillis()
                syncPreferencesHelper.clearSensorPending(sensorId)
                val notBefore = syncPreferencesHelper.recordSensorFailure(sensorId, now)
                Log.w(
                    TAG,
                    "[$sensorId] no ACK progress for " +
                            "${Constants.AutoSync.PENDING_ACK_TIMEOUT_MS / 1000}s with " +
                            "${inFlightChunkEnds.size} chunk(s) in flight, " +
                            "retrying in ${(notBefore - now) / 1000}s"
                )
                break
            }
            ackSignal.awaitChange(seenVersion, waitMs)
        }
        return sentAny
    }

    private fun buildChunk(
        sensorId: String,
        chunkStartTs: Long,
        chunkEndTs: Long,
        data: List<CsvSerializable>
    ): String {
        val csvBuilder = StringBuilder()
        csvBuilder.append("SENSOR:$sensorId\n")
        csvBuilder.append("CHUNK_START_TS:$chunkStartTs\n")
        csvBuilder.append("CHUNK_END_TS:$chunkEndTs\n")
        csvBuilder.append("---DATA---\n")
        csvBuilder.append("$sensorId\n")
        csvBuilder.append(data.first().csvHeader() + "\n")
        data.forEach { entity -> csvBuilder.append(entity.toCsvRow() + "\n") }
        csvBuilder.append("\n")
        return csvBuilder.toString()
    }
}
