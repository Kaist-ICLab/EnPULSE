package kaist.iclab.mobiletracker.config

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Manages the Supabase connection configuration, allowing users to override
 * the default URL built into the application, and holds the Anonymous Key
 * fetched from the server-side `anon_key` edge function (the key is no longer
 * entered manually by the user).
 */
class SupabaseConfigManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "supabase_config_prefs"
        private const val KEY_CUSTOM_URL = "custom_supabase_url"
        private const val KEY_FETCHED_ANON_KEY = "fetched_supabase_anon_key"
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
     * Returns the key last fetched from the server-side edge function if available,
     * otherwise falls back to the build config default.
     */
    fun getAnonKey(): String {
        val fetchedKey = prefs.getString(KEY_FETCHED_ANON_KEY, null)
        return if (!fetchedKey.isNullOrBlank()) fetchedKey else AppConfig.SUPABASE_ANON_KEY
    }

    /**
     * Gets the raw custom URL (can be null/empty) for the UI fields.
     */
    fun getCustomUrlRaw(): String = prefs.getString(KEY_CUSTOM_URL, "") ?: ""

    /**
     * Gets the raw fetched Anon Key (can be empty if never successfully fetched).
     */
    fun getFetchedAnonKeyRaw(): String = prefs.getString(KEY_FETCHED_ANON_KEY, "") ?: ""

    /**
     * Fetches the Anonymous Key from the server-side edge function at
     * `"$url/functions/v1/anon_key"`. The endpoint requires no authentication
     * (it's the bootstrap source of the key itself) and returns the key as a
     * plain-text response body.
     */
    suspend fun fetchAnonKeyFromServer(url: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanedUrl = url.trim().removeSuffix("/")
            val endpoint = URL("$cleanedUrl/functions/v1/anon_key")
            val conn = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
            }
            val code = conn.responseCode
            if (code == 200) {
                val key = conn.inputStream.bufferedReader().use { it.readText() }.trim()
                if (key.isNotBlank()) Result.success(key) else Result.failure(IllegalStateException("Empty anon key response"))
            } else {
                Result.failure(IllegalStateException("HTTP $code"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Persists a freshly-fetched Anonymous Key.
     */
    fun saveFetchedAnonKey(key: String) {
        prefs.edit(commit = true) {
            if (key.isNotBlank()) {
                putString(KEY_FETCHED_ANON_KEY, key.trim())
            } else {
                remove(KEY_FETCHED_ANON_KEY)
            }
        }
    }

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
     * Checks if either a custom user-defined URL OR a valid default configuration is present.
     */
    fun isConfigured(): Boolean {
        val hasCustom = getCustomUrlRaw().isNotBlank()
        return hasCustom || isDefaultConfigured()
    }

    /**
     * Saves a custom Supabase URL. If an empty string is passed, it clears the custom URL.
     */
    fun saveCustomUrl(url: String) {
        prefs.edit(commit = true) {
            if (url.isNotBlank()) {
                putString(KEY_CUSTOM_URL, url.trim())
            } else {
                remove(KEY_CUSTOM_URL)
            }
        }
    }

    /**
     * Clears any custom configuration, reverting back to the defaults.
     */
    fun clearCustomCredentials() {
        prefs.edit(commit = true) { clear() }
    }
}
