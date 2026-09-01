package kaist.iclab.mobiletracker.services

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kaist.iclab.mobiletracker.Constants
import kaist.iclab.mobiletracker.utils.DateTimeFormatter

/**
 * Service for tracking and retrieving sync-related timestamps.
 * Uses SharedPreferences for persistent storage of timestamps.
 */
class SyncTimestampService(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        Constants.Prefs.SYNC_PREFS_NAME,
        Context.MODE_PRIVATE
    )

    /**
     * Update timestamp when watch data is received via BLE
     */
    fun updateLastWatchDataReceived() {
        prefs.edit {
            putLong(Constants.Prefs.KEY_LAST_WATCH_DATA, System.currentTimeMillis())
        }
    }

    /**
     * Update timestamp when phone sensor data is collected
     */
    fun updateLastPhoneSensorData() {
        prefs.edit {
            putLong(Constants.Prefs.KEY_LAST_PHONE_SENSOR, System.currentTimeMillis())
        }
    }

    /**
     * Update timestamp when data is successfully uploaded to server (global)
     * @param timestamp Optional timestamp to use, defaults to current time
     */
    fun updateLastSuccessfulUpload(timestamp: Long = System.currentTimeMillis()) {
        prefs.edit { putLong(Constants.Prefs.KEY_LAST_SUCCESSFUL_UPLOAD, timestamp) }
    }

    /**
     * Update timestamp when a specific sensor's data is successfully uploaded to server.
     * This is purely a display value ("last synced at") — it is NOT the upload-progress
     * watermark; that's [getUploadCursor]/[setUploadCursor].
     * @param sensorId The ID of the sensor that was uploaded
     * @param timestamp Wall-clock time of the successful upload, defaults to now
     */
    fun updateLastSuccessfulUpload(sensorId: String, timestamp: Long = System.currentTimeMillis()) {
        val key = "last_upload_$sensorId"
        prefs.edit { putLong(key, timestamp) }
        // Also update global timestamp
        updateLastSuccessfulUpload(timestamp)
    }

    /**
     * Get the last successful upload timestamp for a specific sensor
     * @param sensorId The ID of the sensor
     * @return Formatted timestamp string, or null if never uploaded
     */
    fun getLastSuccessfulUpload(sensorId: String): String? {
        val key = "last_upload_$sensorId"
        val timestamp = prefs.getLong(key, 0L)
        return if (timestamp > 0) {
            DateTimeFormatter.formatTimestampShort(timestamp)
        } else {
            null
        }
    }

    /**
     * Get the last successful upload timestamp for a specific sensor (raw timestamp)
     * @param sensorId The ID of the sensor
     * @return Timestamp in milliseconds, or null if never uploaded
     */
    fun getLastSuccessfulUploadTimestamp(sensorId: String): Long? {
        val key = "last_upload_$sensorId"
        val timestamp = prefs.getLong(key, 0L)
        return if (timestamp > 0) timestamp else null
    }

    /**
     * Upload-progress cursor for a sensor — the local ObjectBox row id up to which data has been
     * confirmed uploaded to Supabase. Distinct from [getLastSuccessfulUploadTimestamp], which is
     * just a display value; this is what upload gating actually checks against.
     *
     * Deliberately keyed on the local row id rather than the record's own event timestamp: id is
     * assigned in true insertion order, so it stays correct even when data arrives out of order
     * (e.g. a watch BLE reconnect/backfill resending events older than ones already uploaded) — a
     * timestamp-keyed cursor would silently and permanently skip such rows. See
     * [kaist.iclab.mobiletracker.services.upload.handlers.SensorUploadHandlerImpl].
     *
     * Defaults to 0 for sensors that have never set a cursor under this key — including every
     * sensor on every device the first time this runs after the cursor was switched from
     * timestamp-based to id-based, which causes a one-time full re-upload of local data. That's
     * intentional and safe: Supabase upserts by `event_id`, so re-sending already-uploaded rows
     * is a no-op server-side, not a duplicate.
     */
    fun getUploadCursor(sensorId: String): Long =
        prefs.getLong("upload_cursor_id_$sensorId", 0L)

    fun setUploadCursor(sensorId: String, id: Long) {
        prefs.edit { putLong("upload_cursor_id_$sensorId", id) }
    }

    /**
     * Count of [sensorId] records actually accepted by Supabase, derived from [getUploadCursor]
     * (a local row id) minus [UploadStats.quarantinedRecordCount] (batches given up on, not
     * uploaded) — no separate counter to keep in lockstep with every upload success site. This only
     * works because ids are assigned sequentially per box and this app never prunes data, so
     * "highest id processed" and "records processed" stay equal. Doesn't apply to Survey/WebAppLog,
     * which track "already uploaded" per row via an `isSynced` flag instead of a cursor — see
     * [kaist.iclab.mobiletracker.repository.handlers.SurveyResponseDataHandler.getUploadedRecordCount]/
     * [kaist.iclab.mobiletracker.repository.handlers.WebAppLogDataHandler.getUploadedRecordCount],
     * which count that directly instead.
     */
    fun getUploadedRecordCountFromCursor(sensorId: String): Long =
        (getUploadCursor(sensorId) - getUploadStats(sensorId).quarantinedRecordCount).coerceAtLeast(
            0L
        )

    /**
     * How many consecutive upload cycles have ended in a [kaist.iclab.mobiletracker.repository.AppError.ServerRejected]
     * failure — the server explicitly rejecting the request, not a network/auth/timeout hiccup —
     * without the cursor making any progress. Used to detect a "poison" batch that will keep
     * failing forever unless skipped; see [kaist.iclab.mobiletracker.services.upload.handlers.SensorUploadHandlerImpl]'s
     * quarantine logic. Reset to 0 whenever a cycle succeeds or the cursor moves past where it was
     * last recorded (both mean this exact spot is no longer stuck).
     */
    fun getUploadFailureStreak(sensorId: String): Int =
        prefs.getInt("upload_failure_streak_$sensorId", 0)

    private fun getUploadFailureCursor(sensorId: String): Long =
        prefs.getLong("upload_failure_cursor_$sensorId", -1L)

    /**
     * Records that [sensorId]'s upload failed with a server rejection while stuck at [cursor].
     * @return the streak count after this failure (1 the first time [cursor] fails, incrementing
     * only if the *same* [cursor] fails again — a cursor that has moved on gets a fresh streak).
     */
    fun recordUploadFailureAtCursor(sensorId: String, cursor: Long): Int {
        val streak =
            if (getUploadFailureCursor(sensorId) == cursor) getUploadFailureStreak(sensorId) + 1 else 1
        prefs.edit {
            putLong("upload_failure_cursor_$sensorId", cursor)
            putInt("upload_failure_streak_$sensorId", streak)
        }
        return streak
    }

    fun clearUploadFailureStreak(sensorId: String) {
        prefs.edit {
            remove("upload_failure_cursor_$sensorId")
            remove("upload_failure_streak_$sensorId")
        }
    }

    /**
     * Lifetime upload outcome counts for [sensorId], in batches: [succeededBatches] Supabase
     * accepted normally, and [quarantinedBatches] skipped whole after repeated
     * [kaist.iclab.mobiletracker.repository.AppError.ServerRejected] failures at the same spot (see
     * [recordUploadFailureAtCursor]) — those records stay in the local store forever, just never
     * retried. [quarantinedRecordCount] is the same thing in raw record count, for context (a
     * quarantined batch can be up to [kaist.iclab.mobiletracker.Constants.Network.UPLOAD_BATCH_SIZE]
     * records). `null` [successRatePercent] means nothing has been attempted yet.
     */
    data class UploadStats(
        val succeededBatches: Long,
        val quarantinedBatches: Long,
        val quarantinedRecordCount: Long
    ) {
        val successRatePercent: Int?
            get() {
                val total = succeededBatches + quarantinedBatches
                return if (total == 0L) null else ((succeededBatches * 100) / total).toInt()
            }
    }

    fun getUploadStats(sensorId: String): UploadStats = UploadStats(
        succeededBatches = prefs.getLong("upload_succeeded_batches_$sensorId", 0L),
        quarantinedBatches = prefs.getLong("upload_quarantined_batches_$sensorId", 0L),
        quarantinedRecordCount = prefs.getLong("upload_quarantined_records_$sensorId", 0L)
    )

    /**
     * Adds to [sensorId]'s lifetime [UploadStats]. Pass 0 for whichever side didn't happen.
     * @param quarantinedRecordCount How many records were in the quarantined batch (0 when
     * [succeededBatches] is being recorded instead).
     */
    fun addUploadStats(
        sensorId: String,
        succeededBatches: Int = 0,
        quarantinedRecordCount: Int = 0
    ) {
        if (succeededBatches == 0 && quarantinedRecordCount == 0) return
        val current = getUploadStats(sensorId)
        prefs.edit {
            if (succeededBatches != 0) {
                putLong(
                    "upload_succeeded_batches_$sensorId",
                    current.succeededBatches + succeededBatches
                )
            }
            if (quarantinedRecordCount != 0) {
                putLong("upload_quarantined_batches_$sensorId", current.quarantinedBatches + 1)
                putLong(
                    "upload_quarantined_records_$sensorId",
                    current.quarantinedRecordCount + quarantinedRecordCount
                )
            }
        }
    }

    /**
     * Update timestamp when data collection starts
     */
    fun updateDataCollectionStarted() {
        prefs.edit {
            putLong(Constants.Prefs.KEY_DATA_COLLECTION_STARTED, System.currentTimeMillis())
        }
    }

    /**
     * Clear data collection started timestamp (when collection stops)
     */
    fun clearDataCollectionStarted() {
        prefs.edit { remove(Constants.Prefs.KEY_DATA_COLLECTION_STARTED) }
    }

    /**
     * Automatic sync interval in milliseconds.
     */
    fun getAutoSyncIntervalMs(): Long {
        return prefs.getLong(
            Constants.Prefs.KEY_AUTO_SYNC_INTERVAL,
            Constants.AutoSync.INTERVAL_NONE
        )
    }

    fun setAutoSyncIntervalMs(intervalMs: Long) {
        val validIntervals = setOf(
            Constants.AutoSync.INTERVAL_NONE,
            Constants.AutoSync.INTERVAL_5_MIN,
            Constants.AutoSync.INTERVAL_30_MIN,
            Constants.AutoSync.INTERVAL_60_MIN,
            Constants.AutoSync.INTERVAL_2_HOUR,
            Constants.AutoSync.INTERVAL_6_HOUR,
            Constants.AutoSync.INTERVAL_12_HOUR
        )

        prefs.edit {
            putLong(
                Constants.Prefs.KEY_AUTO_SYNC_INTERVAL,
                if (intervalMs in validIntervals) intervalMs else Constants.AutoSync.INTERVAL_NONE
            )
        }
    }

    /**
     * Automatic sync network mode.
     * See AUTO_SYNC_NETWORK_* constants.
     */
    fun getAutoSyncNetworkMode(): Int {
        return prefs.getInt(
            Constants.Prefs.KEY_AUTO_SYNC_NETWORK,
            Constants.AutoSync.NETWORK_WIFI_MOBILE
        )
    }

    fun setAutoSyncNetworkMode(mode: Int) {
        prefs.edit { putInt(Constants.Prefs.KEY_AUTO_SYNC_NETWORK, mode) }
    }

    /**
     * Get formatted last watch data received timestamp
     */
    fun getLastWatchDataReceived(): String? {
        val timestamp = prefs.getLong(Constants.Prefs.KEY_LAST_WATCH_DATA, 0L)
        return if (timestamp > 0) {
            DateTimeFormatter.formatTimestampShort(timestamp)
        } else {
            null
        }
    }

    /**
     * Get formatted last phone sensor data timestamp
     */
    fun getLastPhoneSensorData(): String? {
        val timestamp = prefs.getLong(Constants.Prefs.KEY_LAST_PHONE_SENSOR, 0L)
        return if (timestamp > 0) {
            DateTimeFormatter.formatTimestampShort(timestamp)
        } else {
            null
        }
    }

    /**
     * Get formatted last successful upload timestamp
     */
    fun getLastSuccessfulUpload(): String? {
        val timestamp = prefs.getLong(Constants.Prefs.KEY_LAST_SUCCESSFUL_UPLOAD, 0L)
        return if (timestamp > 0) {
            DateTimeFormatter.formatTimestampShort(timestamp)
        } else {
            null
        }
    }

    /**
     * Get formatted data collection started timestamp
     */
    fun getDataCollectionStarted(): String? {
        val timestamp = prefs.getLong(Constants.Prefs.KEY_DATA_COLLECTION_STARTED, 0L)
        return if (timestamp > 0) {
            DateTimeFormatter.formatTimestampShort(timestamp)
        } else {
            null
        }
    }


    /**
     * Clear the last successful upload timestamp AND upload cursor for a specific sensor (e.g.
     * when the user deletes all local data for it — nothing left to resume from).
     * @param sensorId The ID of the sensor
     */
    fun clearLastSuccessfulUpload(sensorId: String) {
        prefs.edit {
            remove("last_upload_$sensorId")
            remove("upload_cursor_id_$sensorId")
            remove("upload_failure_cursor_$sensorId")
            remove("upload_failure_streak_$sensorId")
            remove("upload_succeeded_batches_$sensorId")
            remove("upload_quarantined_batches_$sensorId")
            remove("upload_quarantined_records_$sensorId")
            remove("uploaded_record_count_$sensorId")
        }
    }

    /**
     * Clear all sensor upload timestamps and cursors.
     * @param existingEditor Optional existing editor to use for atomicity
     */
    fun clearAllSensorUploadTimestamps(existingEditor: SharedPreferences.Editor? = null) {
        val allKeys = prefs.all.keys
        val keysToRemove = allKeys.filter {
            it.startsWith("last_upload_") || it.startsWith("upload_cursor_id_") ||
                    it.startsWith("upload_failure_cursor_") || it.startsWith("upload_failure_streak_") ||
                    it.startsWith("uploaded_record_count_") ||
                    it.startsWith("upload_succeeded_batches_") || it.startsWith("upload_quarantined_batches_") ||
                    it.startsWith("upload_quarantined_records_")
        }
        val editor = existingEditor ?: prefs.edit()
        keysToRemove.forEach { editor.remove(it) }
        if (existingEditor == null) {
            editor.apply()
        }
    }

    /**
     * Clear all sync-related timestamps.
     * This includes:
     * - All per-sensor upload timestamps
     * - Global last successful upload
     * - Last watch data received
     * - Last phone sensor data
     * - Data collection started
     */
    fun clearAllSyncTimestamps() {
        prefs.edit {

            // Clear all per-sensor upload timestamps using the same editor
            clearAllSensorUploadTimestamps(this)

            // Clear global upload timestamp
            remove(Constants.Prefs.KEY_LAST_SUCCESSFUL_UPLOAD)

            // Clear last received timestamps
            remove(Constants.Prefs.KEY_LAST_WATCH_DATA)
            remove(Constants.Prefs.KEY_LAST_PHONE_SENSOR)

            // Clear data collection started
            remove(Constants.Prefs.KEY_DATA_COLLECTION_STARTED)

        }
    }

    /**
     * Get the verified-contiguous watch->phone BLE sync watermark for a sensor.
     * Distinct from [getLastSuccessfulUploadTimestamp]: this tracks how far BLE receipt
     * from the watch has been confirmed gap-free, not how far upload-to-server has progressed.
     */
    fun getBleContiguousTimestamp(sensorId: String): Long? {
        val timestamp = prefs.getLong("ble_contiguous_ts_$sensorId", 0L)
        return if (timestamp > 0) timestamp else null
    }

    /**
     * Set the verified-contiguous watch->phone BLE sync watermark for a sensor.
     */
    fun setBleContiguousTimestamp(sensorId: String, timestamp: Long) {
        prefs.edit { putLong("ble_contiguous_ts_$sensorId", timestamp) }
    }

    /**
     * Store user UUID for use in background operations
     * Called when user successfully logs in
     */
    fun storeUserUuid(uuid: String) {
        prefs.edit {
            putString(Constants.Prefs.KEY_CACHED_USER_UUID, uuid)
        }
    }

    /**
     * Get cached user UUID
     * Returns null if no UUID is cached
     */
    fun getCachedUserUuid(): String? {
        return prefs.getString(Constants.Prefs.KEY_CACHED_USER_UUID, null)
    }

    /**
     * Clear cached user UUID
     * Called when user logs out
     */
    fun clearUserUuid() {
        prefs.edit {
            remove(Constants.Prefs.KEY_CACHED_USER_UUID)
        }
    }
}
