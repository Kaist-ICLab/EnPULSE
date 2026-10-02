package kaist.iclab.tracker.auth

/**
 * Optional capability for [Authentication] implementations that can sign a user in with a
 * plain email and password, alongside whatever federated provider they normally use.
 *
 * Kept separate from [Authentication] so implementations that only support a federated
 * provider (see [GoogleAuth]) stay valid. Callers test for it with `as?` and hide the
 * email/password UI when the active implementation does not provide it.
 *
 * The reason this exists: federated sign-in needs to reach the identity provider, which is
 * not a given on every network a study runs on. Email/password only ever talks to the
 * project's own backend, so it keeps the app usable where the provider is unreachable.
 */
interface EmailPasswordAuthentication {
    /**
     * Sign in with [email] and [password], updating [Authentication.userStateFlow] the same
     * way a federated login would. Implementations report failure through that flow rather
     * than by throwing.
     */
    suspend fun loginWithEmail(email: String, password: String)
}
