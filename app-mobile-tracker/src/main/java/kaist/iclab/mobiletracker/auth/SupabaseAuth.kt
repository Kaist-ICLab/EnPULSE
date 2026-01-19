package kaist.iclab.mobiletracker.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import kaist.iclab.mobiletracker.BuildConfig
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.services.SyncTimestampService
import kaist.iclab.mobiletracker.utils.SupabaseLoadingInterceptor
import kaist.iclab.mobiletracker.utils.SupabaseSessionHelper
import kaist.iclab.tracker.auth.Authentication
import kaist.iclab.tracker.auth.User
import kaist.iclab.tracker.auth.UserState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Supabase-based authentication implementation using Google Sign-In.
 * Uses CredentialManager to get Google ID token and authenticates with Supabase Auth.
 */
class SupabaseAuth(
    private val context: Context,
    private val clientId: String,
    private val supabaseHelper: SupabaseHelper,
    private val syncTimestampService: SyncTimestampService
) : Authentication {

    companion object {
        private const val TAG = "SupabaseAuth"
    }

    private val supabaseClient = supabaseHelper.supabaseClient
    private val credentialManager = CredentialManager.create(context)

    private val _userStateFlow = MutableStateFlow<UserState>(
        UserState(isLoggedIn = false, user = null, token = null)
    )
    override val userStateFlow: StateFlow<UserState> = _userStateFlow.asStateFlow()

    init {
        // Check if we should skip login (for development convenience)
        if (BuildConfig.SKIP_LOGIN) {
            _userStateFlow.value = UserState(
                isLoggedIn = true,
                user = User(
                    email = "dev@localhost",
                    name = "Development User"
                ),
                token = null
            )
        } else {
            // Check for existing session asynchronously to allow Supabase to load persisted session
            checkCurrentSessionAsync()
        }
    }

    /**
     * Check if there's an existing Supabase session asynchronously
     * This allows Supabase to load the persisted session from storage.
     * Supabase-kt automatically persists sessions on Android using SharedPreferences.
     */
    private fun checkCurrentSessionAsync() {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                // Small delay to ensure Supabase client is fully initialized and has loaded persisted session
                delay(200)
                
                // Get current session (Supabase automatically loads from persisted storage)
                val session = supabaseClient.auth.currentSessionOrNull()
                
                if (session != null) {
                    // Store UUID in SharedPreferences for background operations
                    val uuid = SupabaseSessionHelper.getUuidOrNull(supabaseClient)
                    if (uuid != null) {
                        syncTimestampService.storeUserUuid(uuid)
                    }
                    _userStateFlow.value = createUserStateFromSession(session)
                } else {
                    // Clear cached UUID if no session
                    syncTimestampService.clearUserUuid()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking current session: ${e.message}", e)
            }
        }
    }

    override suspend fun getToken() {
        SupabaseLoadingInterceptor.withLoading {
            try {
                supabaseClient.auth.currentSessionOrNull()?.let { session ->
                    val token = SupabaseSessionHelper.getPropertyValue(session, "accessToken") as? String
                    _userStateFlow.value = _userStateFlow.value.copy(token = token)
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
            Log.e(TAG, "Authentication failed: ${e.message}", e)
            
            // Handle specific error cases
            val errorMessage = when {
                // Check for reauth error in both errorMessage and exception message
                e is GetCredentialCancellationException && (
                    e.errorMessage?.contains("reauth", ignoreCase = true) == true ||
                    e.message?.contains("reauth", ignoreCase = true) == true ||
                    e.message?.contains("[16]", ignoreCase = false) == true
                ) -> {
                    "Your Google account needs to be re-authenticated. Please check your Google account settings and try again."
                }
                e is GetCredentialCancellationException -> {
                    "Authentication was cancelled. Please try again."
                }
                else -> {
                    "Authentication failed: ${e.message}"
                }
            }
            
            _userStateFlow.value = createErrorState(errorMessage)
        }
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
     * Extract user name from Supabase user metadata or email
     * Uses reflection to avoid explicit type dependency
     */
    private fun extractUserName(user: Any): String {
        val email = SupabaseSessionHelper.getPropertyValue(user, "email") as? String
        val metadata = SupabaseSessionHelper.getPropertyValue(user, "userMetadata") as? Map<*, *>?

        return metadata?.get("full_name") as? String
            ?: metadata?.get("name") as? String
            ?: email?.substringBefore("@")
            ?: "No name"
    }

    /**
     * Create UserState from Supabase session
     */
    private fun createUserStateFromSession(session: Any): UserState {
        val supabaseUser = SupabaseSessionHelper.getPropertyValue(session, "user")
            ?: return createErrorState("Session has no user")
        
        val accessToken = SupabaseSessionHelper.getPropertyValue(session, "accessToken") as? String
        val email = SupabaseSessionHelper.getPropertyValue(supabaseUser, "email") as? String

        return UserState(
            isLoggedIn = true,
            user = User(
                email = email ?: "No email",
                name = extractUserName(supabaseUser)
            ),
            token = accessToken
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
            _userStateFlow.value = createErrorState("Failed to process Google credential: ${e.message}")
        }
    }

    /**
     * Authenticate with Supabase using Google ID token
     */
    private suspend fun authenticateWithSupabase(googleIdToken: String) {
        SupabaseLoadingInterceptor.withLoading {
            try {
                supabaseClient.auth.signInWith(IDToken) {
                    idToken = googleIdToken
                    provider = Google
                }

                val session = supabaseClient.auth.currentSessionOrNull()
                    ?: throw Exception("Session not available after sign-in")
                
                // Store UUID in SharedPreferences for background operations
                val uuid = SupabaseSessionHelper.getUuidOrNull(supabaseClient)
                if (uuid != null) {
                    syncTimestampService.storeUserUuid(uuid)
                }
                
                _userStateFlow.value = createUserStateFromSession(session)
            } catch (e: Exception) {
                Log.e(TAG, "Error authenticating with Supabase: ${e.message}", e)
                _userStateFlow.value = createErrorState("Supabase authentication failed: ${e.message}")
            }
        }
    }
}

