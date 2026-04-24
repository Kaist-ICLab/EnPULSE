package kaist.iclab.mobiletracker.repository

import kaist.iclab.mobiletracker.data.sensors.phone.ProfileData
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for user profile management.
 * Handles both caching and remote profile operations.
 */
interface UserProfileRepository {
    /**
     * Get cached user profile as StateFlow
     */
    val profileFlow: StateFlow<ProfileData?>

    /**
     * Get current user UUID from session
     */
    fun getCurrentUuid(): String?

    /**
     * Save user profile to cache
     */
    fun saveProfile(profile: ProfileData)

    /**
     * Clear cached user profile
     */
    fun clearProfile()

    /**
     * Update campaign ID for current user and refresh profile
     * @return Result with success or failure
     */
    suspend fun updateCampaignId(campaignId: Int): Result<Unit>

    /**
     * Refresh profile from remote source
     */
    suspend fun refreshProfile(): Result<ProfileData?>

    /**
     * Consolidates refresh of profile, campaign sensors, and surveys into one operation.
     */
    suspend fun syncFullStudyConfig(): Result<ProfileData?>

    /**
     * Create profile if it doesn't exist
     * @param email User email
     * @param campaignId Optional campaign ID
     */
    suspend fun createProfileIfNotExists(email: String, campaignId: Int?): Result<Unit>
}

