package kaist.iclab.mobiletracker.services

import android.content.Context
import android.content.SharedPreferences
import kaist.iclab.mobiletracker.utils.DateTimeFormatter
import androidx.core.content.edit

/**
 * Service for tracking and retrieving sync-related timestamps.
 * Uses SharedPreferences for persistent storage of timestamps.
 */
class SyncTimestampService(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "sync_timestamps",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_LAST_WATCH_DATA = "last_watch_data"
        private const val KEY_LAST_PHONE_SENSOR = "last_phone_sensor"
        private const val KEY_LAST_SUCCESSFUL_UPLOAD = "last_successful_upload"
        private const val KEY_DATA_COLLECTION_STARTED = "data_collection_started"

        // Automatic sync preferences
        private const val KEY_AUTO_SYNC_INTERVAL = "auto_sync_interval"
        private const val KEY_AUTO_SYNC_NETWORK = "auto_sync_network"
        
        // Cached user UUID for background operations
        private const val KEY_CACHED_USER_UUID = "cached_user_uuid"

        // Interval values in milliseconds (0 = no auto sync)
        // 1 minute = 60,000 ms
        const val AUTO_SYNC_INTERVAL_NONE = 0L

        // Sub-minute intervals
        const val AUTO_SYNC_INTERVAL_30_SEC = 30L * 1000   // 30 seconds

        // Minute intervals
        const val AUTO_SYNC_INTERVAL_1_MIN = 60L * 1000    // 1 minute = 60,000 ms
        const val AUTO_SYNC_INTERVAL_15_MIN = 15L * 60 * 1000 // 15 minutes = 900,000 ms
        const val AUTO_SYNC_INTERVAL_30_MIN = 30L * 60 * 1000 // 30 minutes = 1,800,000 ms
        const val AUTO_SYNC_INTERVAL_60_MIN = 60L * 60 * 1000 // 60 minutes = 3,600,000 ms
        const val AUTO_SYNC_INTERVAL_120_MIN = 120L * 60 * 1000 // 120 minutes = 7,200,000 ms

        // Network mode values
        const val AUTO_SYNC_NETWORK_WIFI_MOBILE = 0
        const val AUTO_SYNC_NETWORK_WIFI_ONLY = 1
        const val AUTO_SYNC_NETWORK_MOBILE_ONLY = 2
    }

    /**
     * Update timestamp when watch data is received via BLE
     */
    fun updateLastWatchDataReceived() {
        prefs.edit().putLong(KEY_LAST_WATCH_DATA, System.currentTimeMillis()).apply()
    }

    /**
     * Update timestamp when phone sensor data is collected
     */
    fun updateLastPhoneSensorData() {
        prefs.edit().putLong(KEY_LAST_PHONE_SENSOR, System.currentTimeMillis()).apply()
    }

    /**
     * Update timestamp when data is successfully uploaded to server (global)
     * @param timestamp Optional timestamp to use, defaults to current time
     */
    fun updateLastSuccessfulUpload(timestamp: Long = System.currentTimeMillis()) {
        prefs.edit().putLong(KEY_LAST_SUCCESSFUL_UPLOAD, timestamp).apply()
    }

    /**
     * Update timestamp when a specific sensor's data is successfully uploaded to server
     * @param sensorId The ID of the sensor that was uploaded
     * @param timestamp The timestamp of the last uploaded record (in milliseconds)
     */
    fun updateLastSuccessfulUpload(sensorId: String, timestamp: Long) {
        val key = "last_upload_$sensorId"
        prefs.edit().putLong(key, timestamp).apply()
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
        prefs.edit().putLong(KEY_DATA_COLLECTION_STARTED, System.currentTimeMillis()).apply()
    }

    /**
     * Clear data collection started timestamp (when collection stops)
     */
    fun clearDataCollectionStarted() {
        prefs.edit().remove(KEY_DATA_COLLECTION_STARTED).apply()
    }

    /**
     * Automatic sync interval in milliseconds.
     */
    fun getAutoSyncIntervalMs(): Long {
        return prefs.getLong(KEY_AUTO_SYNC_INTERVAL, AUTO_SYNC_INTERVAL_NONE)
    }

    fun setAutoSyncIntervalMs(intervalMs: Long) {
        val validIntervals = setOf(
            AUTO_SYNC_INTERVAL_NONE,
            AUTO_SYNC_INTERVAL_30_SEC,
            AUTO_SYNC_INTERVAL_1_MIN,
            AUTO_SYNC_INTERVAL_15_MIN,
            AUTO_SYNC_INTERVAL_30_MIN,
            AUTO_SYNC_INTERVAL_60_MIN,
            AUTO_SYNC_INTERVAL_120_MIN
        )

        prefs.edit {
            putLong(
                KEY_AUTO_SYNC_INTERVAL,
                if (intervalMs in validIntervals) intervalMs else AUTO_SYNC_INTERVAL_NONE
            )
        }
    }

    /**
     * Automatic sync network mode.
     * See AUTO_SYNC_NETWORK_* constants.
     */
    fun getAutoSyncNetworkMode(): Int {
        return prefs.getInt(KEY_AUTO_SYNC_NETWORK, AUTO_SYNC_NETWORK_WIFI_MOBILE)
    }

    fun setAutoSyncNetworkMode(mode: Int) {
        prefs.edit().putInt(KEY_AUTO_SYNC_NETWORK, mode).apply()
    }

    /**
     * Get formatted last watch data received timestamp
     */
    fun getLastWatchDataReceived(): String? {
        val timestamp = prefs.getLong(KEY_LAST_WATCH_DATA, 0L)
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
        val timestamp = prefs.getLong(KEY_LAST_PHONE_SENSOR, 0L)
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
        val timestamp = prefs.getLong(KEY_LAST_SUCCESSFUL_UPLOAD, 0L)
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
        val timestamp = prefs.getLong(KEY_DATA_COLLECTION_STARTED, 0L)
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
        prefs.edit().remove(key).apply()
    }

    /**
     * Clear all sensor upload timestamps
     */
    fun clearAllSensorUploadTimestamps() {
        val allKeys = prefs.all.keys
        val keysToRemove = allKeys.filter { it.startsWith("last_upload_") }
        val editor = prefs.edit()
        keysToRemove.forEach { editor.remove(it) }
        editor.apply()
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
        val editor = prefs.edit()

        // Clear all per-sensor upload timestamps
        clearAllSensorUploadTimestamps()

        // Clear global upload timestamp
        editor.remove(KEY_LAST_SUCCESSFUL_UPLOAD)

        // Clear last received timestamps
        editor.remove(KEY_LAST_WATCH_DATA)
        editor.remove(KEY_LAST_PHONE_SENSOR)

        // Clear data collection started
        editor.remove(KEY_DATA_COLLECTION_STARTED)

        editor.apply()
    }

    /**
     * Store user UUID for use in background operations
     * Called when user successfully logs in
     */
    fun storeUserUuid(uuid: String) {
        prefs.edit {
            putString(KEY_CACHED_USER_UUID, uuid)
        }
    }

    /**
     * Get cached user UUID
     * Returns null if no UUID is cached
     */
    fun getCachedUserUuid(): String? {
        return prefs.getString(KEY_CACHED_USER_UUID, null)
    }

    /**
     * Clear cached user UUID
     * Called when user logs out
     */
    fun clearUserUuid() {
        prefs.edit {
            remove(KEY_CACHED_USER_UUID)
        }
    }
}

