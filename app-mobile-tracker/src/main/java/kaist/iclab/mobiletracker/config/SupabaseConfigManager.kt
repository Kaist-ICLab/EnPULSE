package kaist.iclab.mobiletracker.config

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages the Supabase connection configuration, allowing users to override
 * the default URLs and keys built into the application.
 */
class SupabaseConfigManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "supabase_config_prefs"
        private const val KEY_CUSTOM_URL = "custom_supabase_url"
        private const val KEY_CUSTOM_ANON_KEY = "custom_supabase_anon_key"
    }

    /**
     * Gets the currently active Supabase URL.
     * Returns the custom user-defined URL if available, otherwise falls back to the build config default.
     */
    fun getUrl(): String {
        val customUrl = prefs.getString(KEY_CUSTOM_URL, null)
        return if (!customUrl.isNullOrBlank()) customUrl else AppConfig.SUPABASE_URL
    }

    /**
     * Gets the currently active Supabase Anonymous Key.
     * Returns the custom user-defined Key if available, otherwise falls back to the build config default.
     */
    fun getAnonKey(): String {
        val customKey = prefs.getString(KEY_CUSTOM_ANON_KEY, null)
        return if (!customKey.isNullOrBlank()) customKey else AppConfig.SUPABASE_ANON_KEY
    }

    /**
     * Gets the raw custom URL (can be null/empty) for the UI fields.
     */
    fun getCustomUrlRaw(): String = prefs.getString(KEY_CUSTOM_URL, "") ?: ""

    /**
     * Gets the raw custom Anon Key (can be null/empty) for the UI fields.
     */
    fun getCustomAnonKeyRaw(): String = prefs.getString(KEY_CUSTOM_ANON_KEY, "") ?: ""

    /**
     * Checks if the built-in default configuration is valid.
     */
    fun isDefaultConfigured(): Boolean {
        val defaultUrl = AppConfig.SUPABASE_URL
        val defaultKey = AppConfig.SUPABASE_ANON_KEY
        return defaultUrl.isNotBlank() &&
                !defaultUrl.contains("MISSING") &&
                defaultKey.isNotBlank() &&
                !defaultKey.contains("MISSING")
    }

    /**
     * Checks if either a custom user-defined configuration OR a valid default configuration is present.
     */
    fun isConfigured(): Boolean {
        val hasCustom = getCustomUrlRaw().isNotBlank() && getCustomAnonKeyRaw().isNotBlank()
        return hasCustom || isDefaultConfigured()
    }

    /**
     * Saves custom credentials. If empty strings are passed, it clears the custom configuration.
     */
    fun saveCredentials(url: String, anonKey: String) {
        prefs.edit().apply {
            if (url.isNotBlank()) {
                putString(KEY_CUSTOM_URL, url.trim())
            } else {
                remove(KEY_CUSTOM_URL)
            }

            if (anonKey.isNotBlank()) {
                putString(KEY_CUSTOM_ANON_KEY, anonKey.trim())
            } else {
                remove(KEY_CUSTOM_ANON_KEY)
            }
        }.commit()
    }

    /**
     * Clears any custom configurations, reverting back to the defaults.
     */
    fun clearCustomCredentials() {
        prefs.edit().clear().commit()
    }
}
