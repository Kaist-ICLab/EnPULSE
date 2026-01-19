package kaist.iclab.mobiletracker.repository

import kaist.iclab.mobiletracker.data.sensors.phone.ProfileData
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for user profile data caching.
 * Provides in-memory storage for user profile loaded after login.
 */
interface UserProfileRepository {
    /**
     * Get cached user profile as StateFlow
     * @return StateFlow of the cached profile
     */
    val profileFlow: StateFlow<ProfileData?>

    /**
     * Save user profile to cache
     * @param profile The profile to cache
     */
    fun saveProfile(profile: ProfileData)

    /**
     * Clear cached user profile
     */
    fun clearProfile()
}

