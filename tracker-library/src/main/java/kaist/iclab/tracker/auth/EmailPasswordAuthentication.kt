package kaist.iclab.tracker.auth

/**
 * Capability for [Authentication] implementations that can sign a user in with an email and
 * password. Kept separate from [Authentication] so implementations with another sign-in
 * mechanism stay valid; callers test for it with `as?`.
 */
interface EmailPasswordAuthentication {
    /**
     * Sign in with [email] and [password], updating [Authentication.userStateFlow].
     * Implementations report failure through that flow rather than by throwing.
     */
    suspend fun loginWithEmail(email: String, password: String)

    /**
     * Whether the current session was signed in with an email and password. Only such a
     * session has a password that [changePassword] can replace.
     */
    val isPasswordSession: Boolean

    /**
     * Replace the signed-in user's password with [newPassword].
     *
     * @return null on success, otherwise a message to show the user.
     */
    suspend fun changePassword(newPassword: String): String?
}
