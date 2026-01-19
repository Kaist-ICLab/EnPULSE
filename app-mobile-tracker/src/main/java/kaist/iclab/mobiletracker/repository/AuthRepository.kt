package kaist.iclab.mobiletracker.repository

/**
 * Repository interface for authentication data operations.
 * Provides abstraction for authentication token storage and retrieval.
 */
interface AuthRepository {
    /**
     * Save authentication token
     */
    fun saveToken(token: String)

    /**
     * Get authentication token
     * @return The saved token, or null if not found
     */
    fun getToken(): String?

    /**
     * Clear authentication token
     */
    fun clearToken()

    /**
     * Check if a token exists
     */
    fun hasToken(): Boolean
}

