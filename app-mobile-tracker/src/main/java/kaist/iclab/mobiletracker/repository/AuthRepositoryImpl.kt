package kaist.iclab.mobiletracker.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Implementation of AuthRepository using SharedPreferences.
 * Handles storing and retrieving authentication tokens.
 */
class AuthRepositoryImpl(
    private val context: Context
) : AuthRepository {

    companion object {
        private const val PREFS_NAME = "auth_preferences"
        private const val KEY_AUTH_TOKEN = "auth_token"
    }

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    /**
     * Save authentication token to SharedPreferences
     */
    override fun saveToken(token: String) {
        sharedPreferences.edit {
            putString(KEY_AUTH_TOKEN, token)
        }
    }

    /**
     * Get authentication token from SharedPreferences
     * @return The saved token, or null if not found
     */
    override fun getToken(): String? {
        return sharedPreferences.getString(KEY_AUTH_TOKEN, null)
    }

    /**
     * Clear authentication token from SharedPreferences
     */
    override fun clearToken() {
        sharedPreferences.edit {
            remove(KEY_AUTH_TOKEN)
        }
    }

    /**
     * Check if a token exists in SharedPreferences
     */
    override fun hasToken(): Boolean {
        return getToken() != null
    }
}

