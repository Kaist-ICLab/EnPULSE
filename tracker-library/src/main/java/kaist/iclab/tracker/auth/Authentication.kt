package kaist.iclab.tracker.auth

import kotlinx.coroutines.flow.StateFlow

interface Authentication {
    val userStateFlow: StateFlow<UserState>

    suspend fun getToken()

    /**
     * Sign in with [email] and [password]. Implementations report failure through
     * [userStateFlow] rather than by throwing.
     */
    suspend fun loginWithEmail(email: String, password: String)

    suspend fun logout()
}
