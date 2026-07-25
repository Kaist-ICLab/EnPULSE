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
     * Update timestamp when a specific sensor's data is successfully uploaded to server
     * @param sensorId The ID of the sensor that was uploaded
     * @param timestamp The timestamp of the last uploaded record (in milliseconds)
     */
    fun updateLastSuccessfulUpload(sensorId: String, timestamp: Long) {
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
     * Clear the last successful upload timestamp for a specific sensor
     * @param sensorId The ID of the sensor
     */
    fun clearLastSuccessfulUpload(sensorId: String) {
        val key = "last_upload_$sensorId"
        prefs.edit { remove(key) }
    }

    /**
     * Clear all sensor upload timestamps
     * @param existingEditor Optional existing editor to use for atomicity
     */
    fun clearAllSensorUploadTimestamps(existingEditor: SharedPreferences.Editor? = null) {
        val allKeys = prefs.all.keys
        val keysToRemove = allKeys.filter { it.startsWith("last_upload_") }
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
