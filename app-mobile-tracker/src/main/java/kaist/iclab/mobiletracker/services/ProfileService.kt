package kaist.iclab.mobiletracker.services

import android.util.Log
import io.github.jan.supabase.postgrest.from
import kaist.iclab.mobiletracker.data.sensors.phone.ProfileData
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.repository.Result
import kaist.iclab.mobiletracker.repository.runCatchingSuspend
import kaist.iclab.mobiletracker.utils.SupabaseLoadingInterceptor

/**
 * Service for handling profile data operations with Supabase
 */
class ProfileService(
    private val supabaseHelper: SupabaseHelper
) {
    private val supabaseClient = supabaseHelper.supabaseClient
    private val tableName = "profiles"

    companion object {
        private const val TAG = "ProfileService"
    }

    /**
     * Check if a profile exists for the given UUID
     * @param uuid The UUID to check
     * @return Result containing true if exists, false otherwise, or error
     */
    suspend fun profileExists(uuid: String): Result<Boolean> {
        return SupabaseLoadingInterceptor.withLoading {
            runCatchingSuspend {
                try {
                    val response = supabaseClient.from(tableName)
                        .select {
                            filter {
                                eq("uuid", uuid)
                            }
                        }
                    val profiles = response.decodeList<ProfileData>()
                    val exists = profiles.isNotEmpty()
                    exists
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking if profile exists (UUID: $uuid): ${e.message}", e)
                    throw e
                }
            }
        }
    }

    /**
     * Create or update a profile in Supabase
     * If profile exists, it will be updated (upsert behavior)
     * @param profile The profile data to save
     * @return Result containing Unit on success or error
     */
    private suspend fun saveProfile(profile: ProfileData): Result<Unit> {
        return SupabaseLoadingInterceptor.withLoading {
            runCatchingSuspend {
                try {
                    // Use upsert to insert if not exists, update if exists
                    supabaseClient.from(tableName).upsert(profile)
                    Unit // Explicitly return Unit
                } catch (e: Exception) {
                    Log.e(
                        TAG,
                        "Error saving profile (UUID: ${profile.uuid}, Email: ${profile.email}): ${e.message}",
                        e
                    )
                    throw e
                }
            }
        }
    }

    /**
     * Create a profile if it doesn't exist
     * @param uuid The user UUID
     * @param email The user email
     * @param campaignId Optional campaign ID (can be null)
     * @return Result containing Unit on success or error
     */
    suspend fun createProfileIfNotExists(
        uuid: String,
        email: String,
        campaignId: Int? = null
    ): Result<Unit> {
        return SupabaseLoadingInterceptor.withLoading {
            runCatchingSuspend {
                try {
                    // Check if profile exists
                    val existsResult = profileExists(uuid)
                    val exists = when (existsResult) {
                        is Result.Success -> existsResult.data
                        is Result.Error -> throw existsResult.exception
                    }

                    if (!exists) {
                        // Create new profile
                        val profile = ProfileData(
                            uuid = uuid,
                            email = email,
                            campaign_id = campaignId
                        )
                        val saveResult = saveProfile(profile)
                        when (saveResult) {
                            is Result.Success -> {
                                // Profile saved successfully, do nothing here
                            }

                            is Result.Error -> throw saveResult.exception
                        }
                    }
                } catch (e: Exception) {
                    Log.e(
                        TAG,
                        "Error creating profile if not exists (UUID: $uuid, Email: $email): ${e.message}",
                        e
                    )
                    throw e
                }
            }
        }
    }

    /**
     * Get profile by UUID
     * @param uuid The user UUID
     * @return Result containing ProfileData if found, or error
     */
    suspend fun getProfileByUuid(uuid: String): Result<ProfileData> {
        return SupabaseLoadingInterceptor.withLoading {
            runCatchingSuspend {
                try {
                    val response = supabaseClient.from(tableName)
                        .select {
                            filter {
                                eq("uuid", uuid)
                            }
                        }
                    val profiles = response.decodeList<ProfileData>()
                    if (profiles.isEmpty()) {
                        val error = NoSuchElementException("Profile with UUID $uuid not found")
                        Log.e(TAG, "Profile not found: $uuid", error)
                        throw error
                    }
                    profiles.first()
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting profile by UUID ($uuid): ${e.message}", e)
                    throw e
                }
            }
        }
    }

    /**
     * Update campaign ID for an existing profile
     * @param uuid The user UUID
     * @param campaignId The campaign ID to update (can be null to clear)
     * @return Result containing Unit on success or error
     */
    suspend fun updateCampaignId(uuid: String, campaignId: Int?): Result<Unit> {
        return SupabaseLoadingInterceptor.withLoading {
            runCatchingSuspend {
                try {
                    // Get existing profile to preserve email
                    val profileResult = getProfileByUuid(uuid)
                    val existingProfile = when (profileResult) {
                        is Result.Success -> profileResult.data
                        is Result.Error -> throw profileResult.exception
                    }

                    // Update profile with new campaign_id
                    val updatedProfile = existingProfile.copy(campaign_id = campaignId)
                    val saveResult = saveProfile(updatedProfile)
                    when (saveResult) {
                        is Result.Success -> {
                            // Profile updated successfully
                        }

                        is Result.Error -> throw saveResult.exception
                    }
                } catch (e: Exception) {
                    Log.e(
                        TAG,
                        "Error updating campaign ID (UUID: $uuid, Campaign ID: $campaignId): ${e.message}",
                        e
                    )
                    throw e
                }
            }
        }
    }
}

