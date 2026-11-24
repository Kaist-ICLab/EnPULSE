package kaist.iclab.mobiletracker.auth

import android.content.Context
import android.util.Log
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kaist.iclab.mobiletracker.Constants
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.helpers.LanguageHelper
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.services.SyncTimestampService
import kaist.iclab.mobiletracker.utils.SupabaseLoadingInterceptor
import kaist.iclab.mobiletracker.utils.SupabaseSessionHelper
import kaist.iclab.tracker.auth.Authentication
import kaist.iclab.tracker.auth.EmailPasswordAuthentication
import kaist.iclab.tracker.auth.User
import kaist.iclab.tracker.auth.UserState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Duration.Companion.milliseconds

/**
 * Supabase Auth implementation that signs users in with an email and password.
 */
class SupabaseAuth(
    context: Context,
    private val supabaseHelper: SupabaseHelper,
    private val syncTimestampService: SyncTimestampService
) : Authentication, EmailPasswordAuthentication {

    companion object {
        private const val TAG = "SupabaseAuth"
    }

    private val supabaseClient get() = supabaseHelper.supabaseClient
    // Application context: this object outlives the Activity it is created with.
    private val appContext = context.applicationContext
    private val authScope = ProcessLifecycleOwner.get().lifecycleScope

    private val _userStateFlow = MutableStateFlow(
        UserState(isLoggedIn = false, user = null, token = null, isInitializing = true)
    )
    override val userStateFlow: StateFlow<UserState> = _userStateFlow.asStateFlow()

    init {
        observeSessionStatus()
    }

    /**
     * Mirror the Supabase session status into [userStateFlow] for the lifetime of the process.
     *
     * Offline, auth-kt never reports Authenticated for a session it needs to refresh: it stays
     * Initializing (token near expiry) or RefreshFailure (token expired) and retries every
     * 10 s, keeping the session on disk. Both cases fall back to the stored session, so only
     * an explicit NotAuthenticated (sign-out or a rejected refresh token) logs the user out.
     */
    private fun observeSessionStatus() {
        // Bounded so a SessionStatus stuck at Initializing (see SensorUploadService's identical
        // guard) ends the cold-start loading state instead of leaving isInitializing = true.
        authScope.launch {
            val initialized =
                withTimeoutOrNull(Constants.Network.AUTH_AWAIT_INITIALIZATION_TIMEOUT_MS.milliseconds) {
                    supabaseClient.auth.awaitInitialization()
                } != null
            if (!initialized && _userStateFlow.value.isInitializing) {
                Log.w(TAG, "Timed out waiting for Supabase auth to initialize")
                publishLoggedOut()
            }
        }
        authScope.launch {
            // collectLatest moves to the new client's status when the client is rebuilt.
            supabaseHelper.clientFlow.collectLatest { client ->
                client.auth.sessionStatus.collect { status ->
                    try {
                        handleSessionStatus(status)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error handling session status $status: ${e.message}", e)
                    }
                }
            }
        }
    }

    private suspend fun handleSessionStatus(status: SessionStatus) {
        when (status) {
            is SessionStatus.Authenticated -> {
                SupabaseSessionHelper.getUuidOrNull(supabaseClient)?.let {
                    syncTimestampService.storeUserUuid(it)
                }
                _userStateFlow.value = createUserStateFromSession(status.session)
            }

            is SessionStatus.RefreshFailure,
            SessionStatus.Initializing -> {
                if (!_userStateFlow.value.isLoggedIn) publishStoredSession()
            }

            is SessionStatus.NotAuthenticated -> publishLoggedOut()
        }
    }

    /** Show the user as logged in from the session on disk; the cached UUID is left as is. */
    private suspend fun publishStoredSession() {
        val stored = try {
            supabaseClient.auth.sessionManager.loadSession()
        } catch (e: Exception) {
            null
        }
        stored?.let { createUserStateFromSession(it) }
            ?.takeIf { it.isLoggedIn }
            ?.let { _userStateFlow.value = it }
    }

    private fun publishLoggedOut() {
        syncTimestampService.clearUserUuid()
        // Keep a sign-in error message visible instead of replacing it with a blank state.
        val current = _userStateFlow.value
        if (current.isLoggedIn || current.isInitializing) {
            _userStateFlow.value =
                UserState(isLoggedIn = false, user = null, token = null, isInitializing = false)
        }
    }

    /**
     * Waits for auth-kt to leave [SessionStatus.Initializing]. On a cold start with an expired
     * stored token, [publishStoredSession] already reports the user as logged in, but the client
     * has no session until the refresh lands: requests made in that window go out with the anon
     * key and fail as auth errors. Offline, the status moves to RefreshFailure, so callers proceed
     * and see the real network error. Bounded like [observeSessionStatus] so a status stuck at
     * Initializing can't hang the caller.
     */
    override suspend fun awaitSessionReady() {
        withTimeoutOrNull(Constants.Network.AUTH_AWAIT_INITIALIZATION_TIMEOUT_MS.milliseconds) {
            supabaseClient.auth.awaitInitialization()
        } ?: Log.w(TAG, "Timed out waiting for the Supabase session to become ready")
    }

    override suspend fun getToken() {
        SupabaseLoadingInterceptor.withLoading {
            try {
                supabaseClient.auth.currentSessionOrNull()?.let { session ->
                    _userStateFlow.value = _userStateFlow.value.copy(token = session.accessToken)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting token: ${e.message}", e)
            }
        }
    }

    /**
     * Get the user UUID from the current session
     * @return The user UUID
     * @throws IllegalStateException if UUID is not available
     */
    fun getUuid(): String {
        return SupabaseSessionHelper.getUuid(supabaseClient)
    }

    /**
     * Sign in against the configured Supabase instance with an email and password.
     *
     * Requires email/password to be enabled on the Supabase project and the account to
     * already exist; this deliberately does not sign new users up.
     */
    override suspend fun loginWithEmail(email: String, password: String) {
        // Clear the previous error so a retry that fails the same way still shows feedback.
        _userStateFlow.value = _userStateFlow.value.copy(message = null)

        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty() || password.isEmpty()) {
            _userStateFlow.value = createErrorState("Enter both an email and a password.")
            return
        }

        signInAndPublish("Email authentication failed", ::emailLoginErrorMessage) {
            supabaseClient.auth.signInWith(Email) {
                this.email = trimmedEmail
                this.password = password
            }
        }
    }

    private fun emailLoginErrorMessage(e: Exception): String = when {
        e is AuthRestException -> when (e.errorCode) {
            AuthErrorCode.InvalidCredentials -> "Incorrect email or password."
            AuthErrorCode.EmailNotConfirmed -> "This account's email address is not confirmed yet."
            AuthErrorCode.EmailProviderDisabled -> "Email sign-in is not enabled on this server."
            AuthErrorCode.OverRequestRateLimit -> "Too many attempts. Wait a moment and try again."
            else -> "Sign-in failed: ${e.errorDescription}"
        }
        e is HttpRequestException || e is TimeoutCancellationException ->
            "Can't reach the server. Check the connection and try again."
        else -> "Sign-in failed. Please try again."
    }

    override val isPasswordSession: Boolean
        get() = try {
            supabaseClient.auth.currentSessionOrNull()?.user?.appMetadata
                ?.get("provider")?.jsonPrimitive?.contentOrNull == "email"
        } catch (e: Exception) {
            false
        }

    override suspend fun changePassword(newPassword: String): String? {
        return SupabaseLoadingInterceptor.withLoading {
            try {
                supabaseClient.auth.updateUser { password = newPassword }
                null
            } catch (e: Exception) {
                if (e is CancellationException && e !is TimeoutCancellationException) throw e
                Log.e(TAG, "Password change failed: ${e.message}", e)
                changePasswordErrorMessage(e)
            }
        }
    }

    private fun changePasswordErrorMessage(e: Exception): String {
        // Resolved per call, so a language switched while the app is running is respected.
        val localized = LanguageHelper(appContext).applyLanguage(appContext)
        return when (e) {
            is AuthRestException -> when (e.errorCode) {
                AuthErrorCode.WeakPassword ->
                    localized.getString(R.string.change_password_error_weak, e.errorDescription)

                AuthErrorCode.SamePassword ->
                    localized.getString(R.string.change_password_error_same)

                AuthErrorCode.ReauthenticationNeeded ->
                    localized.getString(R.string.change_password_error_reauth)

                AuthErrorCode.OverRequestRateLimit ->
                    localized.getString(R.string.change_password_error_rate_limit)

                else ->
                    localized.getString(R.string.change_password_error_with_detail, e.errorDescription)
            }

            is HttpRequestException, is TimeoutCancellationException -> localized.getString(R.string.change_password_error_network)
            else -> localized.getString(R.string.change_password_error_generic)
        }
    }

    override suspend fun logout() {
        // signOut() keeps the session when the server can't be reached, so remove it locally.
        // Logging out must work with no connection.
        val signedOut = attemptLogoutStep("sign out on the server") {
            SupabaseLoadingInterceptor.withLoading { supabaseClient.auth.signOut() }
        }
        if (!signedOut) {
            attemptLogoutStep("clear the local session") { supabaseClient.auth.clearSession() }
        }

        // Clear cached UUID
        syncTimestampService.clearUserUuid()

        _userStateFlow.value = UserState(
            isLoggedIn = false,
            user = null,
            token = null
        )
    }

    /** Runs one logout step; a failure is logged so the remaining steps still run. */
    private suspend fun attemptLogoutStep(step: String, action: suspend () -> Unit): Boolean {
        return try {
            action()
            true
        } catch (e: Exception) {
            if (e is CancellationException && e !is TimeoutCancellationException) throw e
            Log.w(TAG, "Logout could not $step: ${e.message}", e)
            false
        }
    }

    /**
     * Create UserState from Supabase session using typed SDK access.
     */
    private fun createUserStateFromSession(session: io.github.jan.supabase.auth.user.UserSession): UserState {
        val sessionInfo = SupabaseSessionHelper.getSessionInfo(session)
            ?: return createErrorState("Session has no user")

        return UserState(
            isLoggedIn = true,
            user = User(
                email = sessionInfo.email,
                name = sessionInfo.userName
            ),
            token = sessionInfo.accessToken
        )
    }

    /**
     * Create error UserState
     */
    private fun createErrorState(message: String): UserState {
        return UserState(
            isLoggedIn = false,
            user = null,
            token = null,
            message = message
        )
    }

    /**
     * Run [signIn] behind the loading overlay, then publish the resulting session: cache the
     * UUID for background operations and push the new [UserState]. [errorLabel] prefixes the
     * log line, and [userMessage] turns a failure into the text shown to the user.
     */
    private suspend fun signInAndPublish(
        errorLabel: String,
        userMessage: (Exception) -> String,
        signIn: suspend () -> Unit
    ) {
        SupabaseLoadingInterceptor.withLoading {
            try {
                signIn()

                val session = supabaseClient.auth.currentSessionOrNull()
                    ?: throw Exception("Session not available after sign-in")

                // Store UUID in SharedPreferences for background operations
                val uuid = SupabaseSessionHelper.getUuidOrNull(supabaseClient)
                if (uuid != null) {
                    syncTimestampService.storeUserUuid(uuid)
                }

                _userStateFlow.value = createUserStateFromSession(session)
            } catch (e: Exception) {
                Log.e(TAG, "$errorLabel: ${e.message}", e)
                _userStateFlow.value = createErrorState(userMessage(e))
            }
        }
    }
}

