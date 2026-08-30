package kaist.iclab.wearabletracker.trigger

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import kotlinx.serialization.json.Json

/**
 * Persists the watch survey (question content) configuration to disk. This ensures survey
 * content is preserved across app restarts or device reboots.
 *
 * Renamed from `TriggerConfigStorage` — the trigger engine now lives entirely on the phone (see
 * [kaist.iclab.mobiletracker.di.phone.triggerModule]), so this no longer needs to persist
 * `ParsedCampaignTrigger`/condition data, only the [WatchSurveyConfigPayload] the watch needs to
 * render `WatchSurveyActivity` once told (via [WatchEmaTriggerReceiver]) which survey to launch.
 */
class WatchSurveyConfigStorage(context: Context) {
    companion object {
        private const val TAG = "WatchSurveyConfigStorage"
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
    fun saveConfig(payload: WatchSurveyConfigPayload) {
        try {
            val jsonString = json.encodeToString(payload)
            prefs.edit { putString(KEY_CONFIG, jsonString) }
            Log.d(TAG, "Persisted watch survey config to disk (${jsonString.length} bytes)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist watch survey config: ${e.message}", e)
        }
    }

    /**
     * Load the persisted configuration payload from disk.
     * @return The payload if it exists and is valid, null otherwise.
     */
    fun loadConfig(): WatchSurveyConfigPayload? {
        val jsonString = prefs.getString(KEY_CONFIG, null) ?: return null
        return try {
            json.decodeFromString<WatchSurveyConfigPayload>(jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load persisted watch survey config: ${e.message}", e)
            null
        }
    }

    /**
     * Clear the persisted configuration.
     */
    fun clearConfig() {
        prefs.edit { remove(KEY_CONFIG) }
        Log.d(TAG, "Cleared persisted watch survey config")
    }
}
