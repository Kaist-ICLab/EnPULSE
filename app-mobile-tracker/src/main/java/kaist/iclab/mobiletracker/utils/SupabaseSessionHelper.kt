package kaist.iclab.mobiletracker.utils

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserSession

/**
 * Utility class for extracting information from Supabase sessions.
 * Uses direct typed access to the Supabase SDK instead of reflection.
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
            val uuid = session.user?.id
            if (uuid.isNullOrEmpty()) null else uuid
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
        val session = supabaseClient.auth.currentSessionOrNull()
            ?: throw IllegalStateException("No active Supabase session")

        val user = session.user
            ?: throw IllegalStateException("Session has no user")

        val uuid = user.id
        if (uuid.isEmpty()) {
            throw IllegalStateException("User ID is empty")
        }

        return uuid
    }

    /**
     * Extract user name from Supabase UserInfo.
     * Checks userMetadata for "full_name" or "name", falls back to email prefix.
     */
    fun extractUserName(user: UserInfo): String {
        val metadata = user.userMetadata
        return metadata?.get("full_name")?.toString()?.trim('"')
            ?: metadata?.get("name")?.toString()?.trim('"')
            ?: user.email?.substringBefore("@")
            ?: "No name"
    }

    /**
     * Create a typed accessor for session data.
     * @return Triple of (accessToken, email, userName) or null if session is invalid
     */
    fun getSessionInfo(session: UserSession): SessionInfo? {
        val user = session.user ?: return null
        return SessionInfo(
            accessToken = session.accessToken,
            email = user.email ?: "No email",
            userName = extractUserName(user)
        )
    }

    data class SessionInfo(
        val accessToken: String,
        val email: String,
        val userName: String
    )
}
