package kaist.iclab.mobiletracker.services

import io.github.jan.supabase.postgrest.from
import kaist.iclab.mobiletracker.data.sensors.phone.ProfileData
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.repository.AppError
import kaist.iclab.mobiletracker.repository.ErrorClassifier
import kaist.iclab.mobiletracker.repository.Result
import kaist.iclab.mobiletracker.repository.flatMap
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
            ErrorClassifier.runClassified(TAG, "profileExists($uuid)") {
                val response = supabaseClient.from(tableName)
                    .select {
                        filter {
                            eq("uuid", uuid)
                        }
                    }
                response.decodeList<ProfileData>().isNotEmpty()
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
            ErrorClassifier.runClassified(TAG, "saveProfile(${profile.uuid})") {
                supabaseClient.from(tableName).upsert(profile)
                Unit
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
        return profileExists(uuid).flatMap { exists ->
            if (exists) {
                Result.Success(Unit)
            } else {
                saveProfile(
                    ProfileData(
                        uuid = uuid,
                        email = email,
                        campaignId = campaignId
                    )
                )
            }
        }
    }

    /**
     * Get profile by UUID
     * @param uuid The user UUID
     * @return Result containing ProfileData if found (null if not), or error
     */
    suspend fun getProfileByUuid(uuid: String): Result<ProfileData?> {
        return SupabaseLoadingInterceptor.withLoading {
            ErrorClassifier.runClassified(TAG, "getProfileByUuid($uuid)") {
                val profiles = supabaseClient.from(tableName)
                    .select {
                        filter {
                            eq("uuid", uuid)
                        }
                    }
                    .decodeList<ProfileData>()

                profiles.firstOrNull()
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
        return getProfileByUuid(uuid).flatMap { existingProfile ->
            if (existingProfile == null) {
                Result.Error(AppError.NotFound("Profile with UUID $uuid not found"))
            } else {
                saveProfile(existingProfile.copy(campaignId = campaignId))
            }
        }
    }
}
