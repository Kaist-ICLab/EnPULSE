package kaist.iclab.mobiletracker.auth

import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
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

/**
 * Supabase Auth sign-in with an email and password. Accounts are created on the backend; the
 * app does not sign new users up.
 */
class SupabaseAuth(
    context: Context,
    private val supabaseHelper: SupabaseHelper,
    private val syncTimestampService: SyncTimestampService
) : Authentication {

    companion object {
        private const val TAG = "SupabaseAuth"
    }

    private val supabaseClient get() = supabaseHelper.supabaseClient
    // Application context: this object outlives any Activity.
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
                withTimeoutOrNull(Constants.Network.AUTH_AWAIT_INITIALIZATION_TIMEOUT_MS) {
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
            _userStateFlow.value = createErrorState(string(R.string.login_error_missing_fields))
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
            AuthErrorCode.InvalidCredentials -> string(R.string.login_error_invalid_credentials)
            AuthErrorCode.EmailNotConfirmed -> string(R.string.login_error_email_not_confirmed)
            AuthErrorCode.EmailProviderDisabled -> string(R.string.login_error_email_disabled)
            AuthErrorCode.OverRequestRateLimit -> string(R.string.login_error_rate_limit)
            // The server's description is not translated.
            else -> string(R.string.login_error_with_detail, e.errorDescription)
        }
        e is HttpRequestException || e is TimeoutCancellationException ->
            string(R.string.login_error_network)
        else -> string(R.string.login_error_generic)
    }

    /**
     * A string in the app's chosen language. Resolved on each call so it follows a language
     * change made while the app is running.
     */
    private fun string(@StringRes id: Int, vararg args: Any): String =
        LanguageHelper(appContext).applyLanguage(appContext).getString(id, *args)

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
            ?: return createErrorState(string(R.string.login_error_generic))

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

