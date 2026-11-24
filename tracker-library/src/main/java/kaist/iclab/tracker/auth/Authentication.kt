package kaist.iclab.tracker.auth

import kotlinx.coroutines.flow.StateFlow

interface Authentication {
    val userStateFlow: StateFlow<UserState>

    suspend fun getToken()
    suspend fun logout()

    /**
     * Suspends until a session restored at startup can actually be used for requests: for
     * example, until an expired token has been refreshed. [userStateFlow] may already report the
     * user as logged in before then. Implementations whose session is usable at once keep this
     * default, which returns immediately.
     */
    suspend fun awaitSessionReady() {}
}
