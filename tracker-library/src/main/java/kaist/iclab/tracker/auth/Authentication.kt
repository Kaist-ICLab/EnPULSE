package kaist.iclab.tracker.auth

import kotlinx.coroutines.flow.StateFlow

interface Authentication {
    val userStateFlow: StateFlow<UserState>

    suspend fun getToken()
    suspend fun logout()
}
