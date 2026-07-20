package kaist.iclab.wearabletracker.helpers

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart

/**
 * Helper utility for managing sync metadata using SharedPreferences.
 * Provides a simple API for storing and retrieving sync state.
 */
class SyncPreferencesHelper(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    /**
     * Get the confirmed-synced watermark for a single sensor - the highest timestamp the phone
     * has ACKed as durably (and contiguously) stored. Used as the starting cursor for that
     * sensor's next sync and as the prune point once an ACK confirms it.
     * Returns null if this sensor has never been confirmed synced.
     */
    fun getSensorWatermark(sensorId: String): Long? {
        val timestamp = sharedPreferences.getLong(sensorWatermarkKey(sensorId), -1L)
        return if (timestamp == -1L) null else timestamp
    }

    /**
     * Advance a sensor's confirmed-synced watermark to [timestamp], but only if it's newer than
     * the currently stored value. A stale/duplicate/out-of-order ACK for this sensor is a no-op.
     * Returns whether the watermark actually advanced.
     */
    fun advanceSensorWatermarkIfNewer(sensorId: String, timestamp: Long): Boolean {
        val current = getSensorWatermark(sensorId)
        if (current != null && timestamp <= current) return false
        sharedPreferences.edit {
            putLong(sensorWatermarkKey(sensorId), timestamp)
        }
        return true
    }

    private fun sensorWatermarkKey(sensorId: String) = "$KEY_SENSOR_WATERMARK_PREFIX$sensorId"

    /**
     * Derived "last synced" value for UI display and the auto-sync interval gate: the max
     * confirmed watermark across all sensors, i.e. the most recent confirmation event. Not the
     * min - a single stuck/disabled/new sensor should not stall this indicator or the auto-sync
     * interval gate for every other sensor.
     */
    fun getLastSyncTimestamp(): Long? =
        sharedPreferences.all.entries
            .filter { it.key.startsWith(KEY_SENSOR_WATERMARK_PREFIX) }
            .mapNotNull { it.value as? Long }
            .maxOrNull()

    /**
     * Flow of the derived "last synced" value (see [getLastSyncTimestamp]).
     */
    val lastSyncTimestampFlow: Flow<Long?> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key?.startsWith(KEY_SENSOR_WATERMARK_PREFIX) == true) {
                trySend(getLastSyncTimestamp())
            }
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }.onStart {
        emit(getLastSyncTimestamp())
    }

    /**
     * Timestamp of the last time a sync was *attempted* (not confirmed) - pure cooldown
     * bookkeeping so AutoSyncManager doesn't retrigger faster than one BLE round trip.
     */
    fun getLastSyncAttemptAt(): Long? {
        val timestamp = sharedPreferences.getLong(KEY_LAST_SYNC_ATTEMPT_AT, -1L)
        return if (timestamp == -1L) null else timestamp
    }

    fun saveLastSyncAttemptAt(timestamp: Long) {
        sharedPreferences.edit {
            putLong(KEY_LAST_SYNC_ATTEMPT_AT, timestamp)
        }
    }

    /**
     * Set whether auto-sync is enabled.
     */
    fun setAutoSyncEnabled(enabled: Boolean) {
        sharedPreferences.edit {
            putBoolean(KEY_AUTO_SYNC_ENABLED, enabled)
        }
    }

    /**
     * Check if auto-sync is enabled.
     */
    fun isAutoSyncEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_AUTO_SYNC_ENABLED, false)
    }

    /**
     * Flow of the auto-sync enabled state.
     */
    val autoSyncEnabledFlow: Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == KEY_AUTO_SYNC_ENABLED) {
                trySend(prefs.getBoolean(KEY_AUTO_SYNC_ENABLED, false))
            }
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }.onStart {
        emit(isAutoSyncEnabled())
    }

    /**
     * Set the auto-sync interval in milliseconds.
     */
    fun setAutoSyncInterval(intervalMs: Long) {
        sharedPreferences.edit {
            putLong(KEY_AUTO_SYNC_INTERVAL, intervalMs)
        }
    }

    /**
     * Get the auto-sync interval in milliseconds.
     */
    fun getAutoSyncInterval(): Long {
        return sharedPreferences.getLong(KEY_AUTO_SYNC_INTERVAL, DEFAULT_AUTO_SYNC_INTERVAL_MS)
    }

    /**
     * Flow of the auto-sync interval.
     */
    val autoSyncIntervalFlow: Flow<Long> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == KEY_AUTO_SYNC_INTERVAL) {
                trySend(prefs.getLong(KEY_AUTO_SYNC_INTERVAL, DEFAULT_AUTO_SYNC_INTERVAL_MS))
            }
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }.onStart {
        emit(getAutoSyncInterval())
    }

    companion object {
        private const val PREFS_NAME = "sync_preferences"
        private const val KEY_SENSOR_WATERMARK_PREFIX = "sensor_watermark_"
        private const val KEY_LAST_SYNC_ATTEMPT_AT = "last_sync_attempt_at"

        private const val KEY_AUTO_SYNC_ENABLED = "auto_sync_enabled"
        private const val KEY_AUTO_SYNC_INTERVAL = "auto_sync_interval"

        /** Default auto-sync interval: None (disabled) */
        private const val DEFAULT_AUTO_SYNC_INTERVAL_MS = 0L
    }
}
