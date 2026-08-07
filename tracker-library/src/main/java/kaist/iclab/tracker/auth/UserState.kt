package kaist.iclab.tracker.auth

data class UserState(
    val isLoggedIn: Boolean,
    val user: User? = null,
    val token: String? = null,
    val message: String? = null,
    val isInitializing: Boolean = false
)
