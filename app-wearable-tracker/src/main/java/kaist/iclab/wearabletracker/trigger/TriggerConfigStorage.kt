package kaist.iclab.wearabletracker.trigger

import android.content.Context
import android.util.Log
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Persists the trigger configuration and associated survey configs to disk.
 * This ensures that trigger rules are preserved across app restarts or device reboots.
 */
class TriggerConfigStorage(context: Context) {
    companion object {
        private const val TAG = "TriggerConfigStorage"
        private const val PREFS_NAME = "trigger_config_prefs"
        private const val KEY_CONFIG = "latest_config"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { 
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    /**
     * Persist the configuration payload to SharedPreferences as a JSON string.
     */
    fun saveConfig(payload: TriggerConfigPayload) {
        try {
            val jsonString = json.encodeToString(payload)
            prefs.edit().putString(KEY_CONFIG, jsonString).apply()
            Log.d(TAG, "Persisted trigger config to disk (${jsonString.length} bytes)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist trigger config: ${e.message}", e)
        }
    }

    /**
     * Load the persisted configuration payload from disk.
     * @return The payload if it exists and is valid, null otherwise.
     */
    fun loadConfig(): TriggerConfigPayload? {
        val jsonString = prefs.getString(KEY_CONFIG, null) ?: return null
        return try {
            json.decodeFromString<TriggerConfigPayload>(jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load persisted trigger config: ${e.message}", e)
            null
        }
    }

    /**
     * Clear the persisted configuration.
     */
    fun clearConfig() {
        prefs.edit().remove(KEY_CONFIG).apply()
        Log.d(TAG, "Cleared persisted trigger config")
    }
}
