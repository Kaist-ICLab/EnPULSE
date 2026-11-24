package kaist.iclab.wearabletracker.data

import android.util.Log
import io.objectbox.BoxStore
import kaist.iclab.tracker.sync.ble.BLEDataChannel
import kaist.iclab.wearabletracker.Constants
import kaist.iclab.wearabletracker.db.obx.WatchSensorStore
import kaist.iclab.wearabletracker.helpers.SyncPreferencesHelper
import kaist.iclab.wearabletracker.repository.ErrorClassifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive

/**
 * Confirmed-synced data is deleted immediately on ACK, as before (each sensor's rows at/before
 * the newly-confirmed watermark — see [SyncPreferencesHelper.getSensorWatermark]). On top of
 * that, [pruneIfStorageFull] is an additional backstop, not a replacement: if the ObjectBox
 * database file (now capped at [Constants.DB.OBJECTBOX_MAX_SIZE_KB] instead of ObjectBox's 1 GB
 * default) still crosses [Constants.DB.PRUNE_TRIGGER_BYTES] — e.g. a long BLE outage lets
 * *unsynced* data pile up, or a delete from an earlier ACK didn't get to run — every sensor's
 * already-confirmed-synced data is swept again. In normal operation this is a no-op, since the
 * per-ACK delete already keeps synced data from accumulating.
 */
class SyncAckListener(
    private val bleChannel: BLEDataChannel,
    private val stores: Map<String, WatchSensorStore<*>>,
    private val syncPreferencesHelper: SyncPreferencesHelper,
    private val boxStore: BoxStore,
    private val ackSignal: SyncAckSignal,
    private val coroutineScope: CoroutineScope
) {
    private val TAG = javaClass.simpleName

    fun startListening() {
        bleChannel.addOnReceivedListener(setOf(Constants.BLE.KEY_SYNC_ACK)) { _, jsonElement ->
            val ackData = when (jsonElement) {
                is JsonPrimitive -> jsonElement.content
                else -> jsonElement.toString().trim('"')
            }
            handleAck(ackData)
        }
    }

    /**
     * ACK format: "sensorId:status:endTimestamp" - endTimestamp is the phone's verified-contiguous
     * watermark for that sensor, cumulative and idempotent: a stale/duplicate/out-of-order ACK for
     * a sensor is safe to process any number of times, since advanceSensorWatermarkIfNewer only
     * ever moves forward.
     */
    private fun handleAck(ackData: String) {
        coroutineScope.launch {
            ErrorClassifier.runClassified(TAG, "handle sync ACK") {
                val parts = ackData.split(":")
                if (parts.size < 2) {
                    throw IllegalArgumentException("Invalid ACK format: $ackData")
                }

                val sensorId = parts[0]
                val status = parts[1]
                val endTimestamp = if (parts.size >= 3) parts[2].toLongOrNull() else null

                if (status != "OK") {
                    // Nothing to confirm. Drop the in-flight marker and back this sensor off
                    // exponentially, so a lasting cause (unknown sensor, phone storage full) is
                    // retried at 1, 2, 4 ... 60 min instead of on every sync.
                    syncPreferencesHelper.clearSensorPending(sensorId)
                    val now = System.currentTimeMillis()
                    val notBefore = syncPreferencesHelper.recordSensorFailure(sensorId, now)
                    Log.e(
                        TAG,
                        "Received non-OK ACK for $sensorId: $status, " +
                                "retrying in ${(notBefore - now) / 1000}s"
                    )
                    ackSignal.onAckHandled(sensorId, ok = false, now = now)
                    return@runClassified
                }

                try {
                    handleOkAck(sensorId, endTimestamp)
                } finally {
                    ackSignal.onAckHandled(sensorId, ok = true)
                }
            }
        }
    }

    private fun handleOkAck(sensorId: String, endTimestamp: Long?) {
        if (endTimestamp == null) {
            Log.w(TAG, "Received OK ACK with no timestamp for $sensorId, nothing to confirm")
            return
        }

        val store = stores[sensorId]
        if (store == null) {
            Log.w(TAG, "Received ACK for unknown sensorId: $sensorId")
            return
        }

        if (syncPreferencesHelper.advanceSensorWatermarkIfNewer(sensorId, endTimestamp)) {
            store.deleteDataBefore(endTimestamp)
            syncPreferencesHelper.resetSensorFailures(sensorId)
            // Progress: if chunks past this ACK are still in flight, restart their timeout so a
            // backlog that is being ACKed steadily is never treated as lost.
            syncPreferencesHelper.getSensorPending(sensorId)?.let { pending ->
                if (pending.upToTimestamp > endTimestamp) {
                    syncPreferencesHelper.setSensorPending(
                        sensorId,
                        pending.upToTimestamp,
                        System.currentTimeMillis()
                    )
                }
            }
        } else {
            Log.d(TAG, "Stale/duplicate ACK for $sensorId at $endTimestamp, ignoring")
        }

        // Whether or not the watermark itself advanced (a stale/duplicate ACK's
        // endTimestamp is by definition <= the current watermark), this ACK confirms the
        // phone has processed up through endTimestamp - clear the in-flight marker if
        // it's now covered so PhoneCommunicationManager stops skipping past it.
        syncPreferencesHelper.clearSensorPendingIfCovered(sensorId, endTimestamp)

        pruneIfStorageFull()
    }

    /**
     * Prunes every sensor's already-confirmed-synced data (at/before its watermark) once the
     * ObjectBox database file crosses [Constants.DB.PRUNE_TRIGGER_BYTES]. Cheap to call on every
     * ACK: [BoxStore.getDbSizeOnDisk] is just a stat on the store's file, not a full scan, and
     * below the threshold this is a no-op.
     */
    private fun pruneIfStorageFull() {
        val dbSize = boxStore.getDbSizeOnDisk()
        if (dbSize < Constants.DB.PRUNE_TRIGGER_BYTES) return

        Log.w(
            TAG,
            "ObjectBox DB size ${dbSize / (1024 * 1024)}MB crossed prune threshold " +
                    "${Constants.DB.PRUNE_TRIGGER_BYTES / (1024 * 1024)}MB, pruning confirmed-synced data"
        )
        stores.forEach { (sensorId, store) ->
            val watermark = syncPreferencesHelper.getSensorWatermark(sensorId) ?: return@forEach
            store.deleteDataBefore(watermark)
        }
    }
}
