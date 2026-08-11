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
                    Log.e(TAG, "Received non-OK ACK for $sensorId: $status")
                    // Nothing to confirm, but the phone has definitively told us this attempt
                    // failed - drop the in-flight marker so the next sync attempt retries right
                    // away instead of waiting out PENDING_ACK_TIMEOUT_MS.
                    syncPreferencesHelper.clearSensorPending(sensorId)
                    return@runClassified
                }

                if (endTimestamp == null) {
                    Log.w(TAG, "Received OK ACK with no timestamp for $sensorId, nothing to confirm")
                    return@runClassified
                }

                val store = stores[sensorId]
                if (store == null) {
                    Log.w(TAG, "Received ACK for unknown sensorId: $sensorId")
                    return@runClassified
                }

                if (syncPreferencesHelper.advanceSensorWatermarkIfNewer(sensorId, endTimestamp)) {
                    Log.d(TAG, "Pruning $sensorId data up to: $endTimestamp")
                    store.deleteDataBefore(endTimestamp)
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
        }
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

    fun cleanup() {}
}
