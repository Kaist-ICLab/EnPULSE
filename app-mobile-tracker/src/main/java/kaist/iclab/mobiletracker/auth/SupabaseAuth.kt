package kaist.iclab.mobiletracker.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import kaist.iclab.mobiletracker.Constants
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
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Supabase-based authentication implementation using Google Sign-In.
 * Uses CredentialManager to get Google ID token and authenticates with Supabase Auth.
 *
 * Also implements [EmailPasswordAuthentication] as a fallback for networks where Google's
 * identity endpoints are unreachable but the project's own Supabase instance is not: the
 * Google path needs CredentialManager to talk to Google, whereas [loginWithEmail] only ever
 * talks to the configured backend.
 */
class SupabaseAuth(
    context: Context,
    private val clientId: String,
    supabaseHelper: SupabaseHelper,
    private val syncTimestampService: SyncTimestampService
) : Authentication, EmailPasswordAuthentication {

    companion object {
        private const val TAG = "SupabaseAuth"
    }

    private val supabaseClient = supabaseHelper.supabaseClient
    private val credentialManager = CredentialManager.create(context)
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
            supabaseClient.auth.sessionStatus.collect { status ->
                try {
                    handleSessionStatus(status)
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling session status $status: ${e.message}", e)
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

    override suspend fun login(activity: Activity) {
        val request = buildGoogleIdTokenCredentialRequest()
        try {
            val result = credentialManager.getCredential(
                request = request,
                context = activity
            )
            handleCredential(result.credential)
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Login failed: ${e.message}", e)

            // Handle specific error cases
            // Check for reauth error in both errorMessage and exception message
            val errorMessage = when (e) {
                is GetCredentialCancellationException if (
                        e.errorMessage?.contains("reauth", ignoreCase = true) == true ||
                                e.message?.contains("reauth", ignoreCase = true) == true ||
                                e.message?.contains("[16]", ignoreCase = false) == true
                        ) -> {
                    "Login failed. Please try again"
                }

                is GetCredentialCancellationException -> {
                    "Login was cancelled. Please try again."
                }

                else -> {
                    "Login failed: ${e.message}"
                }
            }

            _userStateFlow.value = createErrorState(errorMessage)
        }
    }

    /**
     * Sign in against the configured Supabase instance with an email and password, with no
     * Google round trip anywhere in the path. Intended for networks that can reach the
     * backend but not Google's identity endpoints.
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

    override suspend fun logout() {
        SupabaseLoadingInterceptor.withLoading {
            try {
                // Sign out from Supabase
                supabaseClient.auth.signOut()

                // Clear credential state from all credential providers
                credentialManager.clearCredentialState(
                    androidx.credentials.ClearCredentialStateRequest()
                )

                // Clear cached UUID
                syncTimestampService.clearUserUuid()

                _userStateFlow.value = UserState(
                    isLoggedIn = false,
                    user = null,
                    token = null
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error during logout: ${e.message}", e)
            }
        }
    }

    /**
     * Build the Google ID token credential request
     */
    private fun buildGoogleIdTokenCredentialRequest(): GetCredentialRequest {
        val googleIdOption = GetSignInWithGoogleOption.Builder(clientId).build()
        return GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
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
     * Handle the credential from CredentialManager and authenticate with Supabase
     */
    private suspend fun handleCredential(credential: Credential) {
        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            Log.e(TAG, "Unexpected credential type: ${credential::class.simpleName}")
            _userStateFlow.value = createErrorState("Unexpected credential type")
            return
        }

        try {
            val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
            authenticateWithSupabase(googleCredential.idToken)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing Google credential: ${e.message}", e)
            _userStateFlow.value =
                createErrorState("Failed to process Google credential: ${e.message}")
        }
    }

    /**
     * Authenticate with Supabase using Google ID token
     */
    private suspend fun authenticateWithSupabase(googleIdToken: String) {
        signInAndPublish("Supabase authentication failed") {
            supabaseClient.auth.signInWith(IDToken) {
                idToken = googleIdToken
                provider = Google
            }
        }
    }

    /**
     * Run [signIn] behind the loading overlay, then publish the resulting session: cache the
     * UUID for background operations and push the new [UserState]. Shared by every sign-in
     * path so they all persist and report a session identically; [errorLabel] prefixes the
     * log line, and [userMessage] turns a failure into the text shown to the user.
     */
    private suspend fun signInAndPublish(
        errorLabel: String,
        userMessage: (Exception) -> String = { "$errorLabel: ${it.message}" },
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

