package kaist.iclab.mobiletracker.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

/**
 * Implementation of AuthRepository using EncryptedSharedPreferences.
 * Handles storing and retrieving authentication tokens securely.
 */
class AuthRepositoryImpl(
    private val context: Context
) : AuthRepository {

    companion object {
        private const val TAG = "AuthRepositoryImpl"
        private const val PREFS_NAME = "auth_preferences_encrypted"
        private const val KEY_AUTH_TOKEN = "auth_token"
    }

    private val sharedPreferences: SharedPreferences = try {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            PREFS_NAME,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Fallback to regular SharedPreferences if encryption fails (e.g., on rooted devices)
        Log.e(TAG, "Failed to create EncryptedSharedPreferences, falling back to plain", e)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Save authentication token securely
     */
    override fun saveToken(token: String) {
        sharedPreferences.edit {
            putString(KEY_AUTH_TOKEN, token)
        }
    }

    /**
     * Get authentication token
     * @return The saved token, or null if not found
     */
    override fun getToken(): String? {
        return sharedPreferences.getString(KEY_AUTH_TOKEN, null)
    }

    /**
     * Clear authentication token
     */
    override fun clearToken() {
        sharedPreferences.edit {
            remove(KEY_AUTH_TOKEN)
        }
    }

    /**
     * Check if a token exists
     */
    override fun hasToken(): Boolean {
        return getToken() != null
    }
}
