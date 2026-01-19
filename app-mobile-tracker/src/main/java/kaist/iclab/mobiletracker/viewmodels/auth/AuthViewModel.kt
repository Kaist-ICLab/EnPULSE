package kaist.iclab.mobiletracker.viewmodels.auth

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kaist.iclab.mobiletracker.auth.SupabaseAuth
import kaist.iclab.mobiletracker.repository.AuthRepository
import kaist.iclab.mobiletracker.repository.Result
import kaist.iclab.mobiletracker.repository.UserProfileRepository
import kaist.iclab.mobiletracker.services.ProfileService
import kaist.iclab.tracker.auth.Authentication
import kaist.iclab.tracker.auth.UserState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authentication: Authentication,
    private val authRepository: AuthRepository,
    private val profileService: ProfileService,
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {
    private val TAG = "AuthViewModel"

    val userState: StateFlow<UserState> = authentication.userStateFlow

    // Expose cached profile from repository
    val userProfile: StateFlow<kaist.iclab.mobiletracker.data.sensors.phone.ProfileData?> =
        userProfileRepository.profileFlow

    private var previousLoginState = false
    private var lastSavedToken: String? = null

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

                    // Save profile to profiles table if not exists, then load and cache it
                    saveProfileIfNotExists(state)
                    loadUserProfile()
                }

                // Clear profile when user logs out
                if (!state.isLoggedIn) {
                    userProfileRepository.clearProfile()
                    previousLoginState = false
                    lastSavedToken = null
                }
            }
        }
    }

    /**
     * Save user profile to profiles table if it doesn't exist
     * Gets UUID from Supabase session and email from user state
     */
    private suspend fun saveProfileIfNotExists(state: UserState) {
        val user = state.user
        if (user == null || user.email.isEmpty()) {
            return
        }

        try {
            // Get UUID from Supabase session
            val uuid = getUuidFromSession()
            if (uuid == null) {
                return
            }

            val email = user.email

            // Save profile if not exists (campaign_id will be null initially)
            when (val result = profileService.createProfileIfNotExists(uuid, email, null)) {
                is Result.Success -> {
                    // Profile saved successfully
                }

                is Result.Error -> {
                    Log.e(TAG, "Error saving profile: ${result.message}", result.exception)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in saveProfileIfNotExists: ${e.message}", e)
        }
    }

    /**
     * Load user profile from Supabase and cache it
     * Called after successful login to make profile data available throughout the app
     */
    private fun loadUserProfile() {
        viewModelScope.launch {
            try {
                val uuid = getUuidFromSession()
                if (uuid == null) {
                    return@launch
                }

                when (val result = profileService.getProfileByUuid(uuid)) {
                    is Result.Success -> {
                        userProfileRepository.saveProfile(result.data)
                    }

                    is Result.Error -> {
                        // Profile might not exist yet, which is okay
                        // Only log if it's not a "not found" error
                        if (result.exception !is NoSuchElementException) {
                            Log.e(
                                TAG,
                                "Error loading user profile: ${result.message}",
                                result.exception
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in loadUserProfile: ${e.message}", e)
            }
        }
    }

    /**
     * Get UUID from Supabase session
     * Gets UUID from SupabaseAuth
     * Returns null if UUID cannot be retrieved (error is logged in SupabaseAuth)
     */
    private suspend fun getUuidFromSession(): String? {
        return try {
            (authentication as? SupabaseAuth)?.getUuid()
        } catch (e: Exception) {
            // Error is already logged in SupabaseAuth.getUuid()
            null
        }
    }

    fun login(activity: Activity) {
        viewModelScope.launch {
            try {
                authentication.login(activity)
            } catch (e: Exception) {
                Log.e(TAG, "Login error: ${e.message}", e)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authentication.logout()
            authRepository.clearToken()
            userProfileRepository.clearProfile() // Clear cached profile on logout
        }
    }

    /**
     * Refresh user profile from Supabase
     * Useful when profile data might have changed (e.g., campaign updated)
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

