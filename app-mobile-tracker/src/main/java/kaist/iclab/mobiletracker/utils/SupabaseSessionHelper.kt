package kaist.iclab.mobiletracker.utils

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth

/**
 * Utility class for extracting information from Supabase sessions.
 * Provides helper functions to get user UUID and other session data using reflection.
 */
object SupabaseSessionHelper {
    private const val TAG = "SupabaseSessionHelper"

    /**
     * Get UUID from Supabase session (nullable version)
     * Returns null if UUID cannot be retrieved
     * @param supabaseClient The Supabase client
     * @return The user UUID, or null if not available
     */
    fun getUuidOrNull(supabaseClient: SupabaseClient): String? {
        return try {
            val session = supabaseClient.auth.currentSessionOrNull() ?: return null

            val user = getPropertyValue(session, "user") ?: return null

            val uuid = getPropertyValue(user, "id") as? String
            if (uuid == null || uuid.isEmpty()) {
                return null
            }

            uuid
        } catch (e: Exception) {
            Log.e(TAG, "Error getting UUID from session: ${e.message}", e)
            null
        }
    }

    /**
     * Get UUID from Supabase session (throws exception version)
     * Throws IllegalStateException if UUID cannot be retrieved
     * @param supabaseClient The Supabase client
     * @return The user UUID
     * @throws IllegalStateException if UUID is not available
     */
    fun getUuid(supabaseClient: SupabaseClient): String {
        return try {
            val session = supabaseClient.auth.currentSessionOrNull()
            if (session == null) {
                Log.e(TAG, "Cannot get UUID: No active session")
                throw IllegalStateException("No active Supabase session")
            }

            val user = getPropertyValue(session, "user")
            if (user == null) {
                Log.e(TAG, "Cannot get UUID: Session has no user")
                throw IllegalStateException("Session has no user")
            }

            val uuid = getPropertyValue(user, "id") as? String
            if (uuid == null || uuid.isEmpty()) {
                Log.e(TAG, "Cannot get UUID: User ID is null or empty")
                throw IllegalStateException("User ID is null or empty")
            }

            uuid
        } catch (e: IllegalStateException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user UUID: ${e.message}", e)
            throw IllegalStateException("Failed to get user UUID: ${e.message}", e)
        }
    }

    /**
     * Helper function to safely get a property value using reflection.
     * Tries getter method first (e.g., getEmail), then direct property access (e.g., email).
     * @param obj The object to get the property from
     * @param propertyName The name of the property to get
     * @return The property value, or null if not found
     */
    fun getPropertyValue(obj: Any, propertyName: String): Any? {
        return try {
            val capitalized = propertyName.replaceFirstChar { it.uppercaseChar() }
            obj.javaClass.getMethod("get$capitalized").invoke(obj)
        } catch (e: NoSuchMethodException) {
            try {
                obj.javaClass.getMethod(propertyName).invoke(obj)
            } catch (e2: Exception) {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}

