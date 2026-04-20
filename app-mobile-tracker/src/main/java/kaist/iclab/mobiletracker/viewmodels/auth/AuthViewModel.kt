package kaist.iclab.mobiletracker.viewmodels.auth

import android.app.Activity
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.data.sensors.phone.ProfileData
import kaist.iclab.mobiletracker.repository.AuthRepository
import kaist.iclab.mobiletracker.repository.CampaignSensorRepository
import kaist.iclab.mobiletracker.repository.Result
import kaist.iclab.mobiletracker.repository.UserProfileRepository
import kaist.iclab.mobiletracker.repository.onFailure
import kaist.iclab.tracker.auth.Authentication
import kaist.iclab.tracker.auth.UserState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for authentication and user profile management.
 *
 * Handles Google Sign-In authentication flow, token management, and user profile
 * synchronization with Supabase. Automatically:
 * - Requests token after successful login
 * - Saves token to repository for persistence
 * - Creates user profile in Supabase if not exists
 * - Loads and caches user profile data
 * - Clears profile data on logout
 *
 * @param authentication Authentication wrapper for login/logout operations
 * @param authRepository Repository for token persistence
 * @param userProfileRepository Repository for user profile data
 */
class AuthViewModel(
    private val authentication: Authentication,
    private val authRepository: AuthRepository,
    private val userProfileRepository: UserProfileRepository,
    private val campaignSensorRepository: CampaignSensorRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val TAG = "AuthViewModel"

    val userState: StateFlow<UserState> = authentication.userStateFlow

    // SharedFlow for UI events (like error toasts)
    private val _uiEvent = MutableSharedFlow<AuthUiEvent>()
    val uiEvent: SharedFlow<AuthUiEvent> = _uiEvent.asSharedFlow()

    // Expose cached profile from repository
    val userProfile: StateFlow<ProfileData?> =
        userProfileRepository.profileFlow

    private var previousLoginState: Boolean
        get() = savedStateHandle[KEY_PREVIOUS_LOGIN_STATE] ?: false
        set(value) {
            savedStateHandle[KEY_PREVIOUS_LOGIN_STATE] = value
        }
    private var lastSavedToken: String? = null

    companion object {
        private const val KEY_PREVIOUS_LOGIN_STATE = "previous_login_state"
    }

    init {
        // Load profile if user is already logged in (e.g., app restart)
        viewModelScope.launch {
            if (userState.value.isLoggedIn) {
                loadUserProfile()
            }
        }

        // Observe userState changes to get token after login and save it automatically
        viewModelScope.launch {
            userState.collect { state ->
                val currentToken = state.token

                // When user successfully logs in, automatically get token
                if (state.isLoggedIn && !previousLoginState && currentToken == null) {
                    previousLoginState = true
                    authentication.getToken()
                }

                // When token becomes available, save it to repository and log it (only once per token)
                if (state.isLoggedIn && currentToken != null && currentToken != lastSavedToken) {
                    authRepository.saveToken(currentToken)
                    lastSavedToken = currentToken

                    // Save profile to profiles table if not exists
                    saveProfileIfNotExists(state)
                    // Then load and cache it (sequentially)
                    loadUserProfileSuspend()
                }

                // Clear profile when user logs out
                if (!state.isLoggedIn) {
                    userProfileRepository.clearProfile()
                    campaignSensorRepository.clearCache()
                    previousLoginState = false
                    lastSavedToken = null
                }
            }
        }
    }

    /**
     * Save user profile to profiles table if it doesn't exist
     */
    private suspend fun saveProfileIfNotExists(state: UserState) {
        val user = state.user
        if (user == null || user.email.isEmpty()) {
            return
        }

        userProfileRepository.createProfileIfNotExists(user.email, null)
            .onFailure { e ->
                Log.e(TAG, "Error saving profile: ${e.message}", e)
                _uiEvent.emit(AuthUiEvent.ShowError(R.string.toast_profile_setup_failed))
            }
    }

    /**
     * Load user profile from Supabase and cache it (non-suspending wrapper)
     */
    fun loadUserProfile() {
        viewModelScope.launch {
            loadUserProfileSuspend()
        }
    }

    /**
     * Suspending function to load user profile.
     */
    private suspend fun loadUserProfileSuspend() {
        when (val result = userProfileRepository.refreshProfile()) {
            is Result.Success -> {
                val profile = result.data
                if (profile?.campaignId == null) {
                    campaignSensorRepository.clearCache()
                }
            }

            is Result.Error -> {
                Log.e(TAG, "Error loading user profile: ${result.message}", result.exception)
                _uiEvent.emit(AuthUiEvent.ShowError(R.string.toast_profile_setup_failed))
            }
        }
    }

    fun login(activity: Activity) {
        viewModelScope.launch {
            try {
                authentication.login(activity)
            } catch (e: Exception) {
                Log.e(TAG, "Login error: ${e.message}", e)
                _uiEvent.emit(AuthUiEvent.ShowError(R.string.toast_login_failed))
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authentication.logout()
            authRepository.clearToken()
            userProfileRepository.clearProfile()
        }
    }

    /**
     * Refresh user profile from Supabase
     */
    fun refreshUserProfile() {
        loadUserProfile()
    }

    fun getToken() {
        viewModelScope.launch {
            authentication.getToken()
        }
    }
}

/**
 * UI events for authentication and profile operations
 */
sealed class AuthUiEvent {
    data class ShowError(val messageResId: Int) : AuthUiEvent()
}
