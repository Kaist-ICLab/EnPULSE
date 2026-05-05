package kaist.iclab.mobiletracker.repository

import kaist.iclab.mobiletracker.data.sensors.phone.ProfileData
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.services.ProfileService
import kaist.iclab.mobiletracker.services.TriggerConfigPusher
import kaist.iclab.mobiletracker.utils.SupabaseSessionHelper
import kaist.iclab.tracker.sensor.controller.BackgroundController
import kaist.iclab.tracker.sensor.controller.ControllerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Implementation of UserProfileRepository.
 * Manages user profile caching and remote operations via ProfileService.
 */
class UserProfileRepositoryImpl(
    private val profileService: ProfileService,
    private val supabaseHelper: SupabaseHelper,
    private val persistentStorage: kaist.iclab.mobiletracker.storage.UserProfileStorage,
    private val campaignSensorRepository: CampaignSensorRepository,
    private val surveyRepository: SurveyRepository,
    private val triggerRepository: TriggerRepository,
    private val triggerConfigPusher: TriggerConfigPusher,
    private val backgroundController: BackgroundController
) : UserProfileRepository {

    companion object {
        private const val TAG = "UserProfileRepo"
    }

    private val _profile = MutableStateFlow<ProfileData?>(
        persistentStorage.get().takeIf { it.uuid.isNotEmpty() }
    )
    override val profileFlow: StateFlow<ProfileData?> = _profile.asStateFlow()

    override fun getCurrentUuid(): String? {
        return _profile.value?.uuid?.takeIf { it.isNotEmpty() }
            ?: SupabaseSessionHelper.getUuidOrNull(supabaseHelper.supabaseClient)
    }

    override fun saveProfile(profile: ProfileData) {
        _profile.value = profile
        persistentStorage.set(profile)
    }

    override fun clearProfile() {
        _profile.value = null
        persistentStorage.set(ProfileData())
        campaignSensorRepository.clearCache()
        surveyRepository.clearSurveys()
        triggerRepository.clearTriggers()
    }

    override suspend fun updateCampaignId(campaignId: Int): Result<Unit> {
        if (backgroundController.controllerStateFlow.value.flag == ControllerState.FLAG.RUNNING) {
            return Result.Error(AppError.CollectionRunning("Cannot update campaign while data collection is running"))
        }

        val uuid = getCurrentUuid()
            ?: return Result.Error(AppError.Unknown("User not logged in"))
        return profileService.updateCampaignId(uuid, campaignId)
    }

    override suspend fun refreshProfile(): Result<ProfileData?> {
        if (backgroundController.controllerStateFlow.value.flag == ControllerState.FLAG.RUNNING) {
            return Result.Error(AppError.CollectionRunning("Cannot refresh profile while data collection is running"))
        }

        val uuid = getCurrentUuid()
            ?: return Result.Error(AppError.Unknown("User not logged in"))

        return when (val result = profileService.getProfileByUuid(uuid)) {
            is Result.Success -> {
                _profile.value = result.data
                result.data?.let { persistentStorage.set(it) }
                result
            }

            is Result.Error -> result
        }
    }

    override suspend fun syncFullStudyConfig(): Result<ProfileData?> {
        // 0. Check if data collection is running
        if (backgroundController.controllerStateFlow.value.flag == ControllerState.FLAG.RUNNING) {
            return Result.Error(AppError.CollectionRunning("Cannot sync config while data collection is running"))
        }

        // 1. Refresh profile first to get latest campaign ID
        val profileResult = refreshProfile()
        if (profileResult is Result.Error) return profileResult

        val profile = profileResult.getOrNull()
        val campaignId = profile?.campaignId

        // 2. If we have a campaign, fetch sensors, surveys, and triggers
        if (campaignId != null) {
            campaignSensorRepository.fetchActiveSensors(campaignId.toLong())
            surveyRepository.fetchAndPersistSurveys(campaignId)
            triggerRepository.fetchAndPersistTriggers(campaignId)

            // 3. Push trigger config to watch via BLE
            val triggers = triggerRepository.triggersFlow.value.triggers
            val surveys = surveyRepository.surveysFlow.value.configs
            if (triggers.isNotEmpty()) {
                triggerConfigPusher.pushToWatch(triggers, surveys)
            }
        } else {
            // If no campaign, clear caches to be safe
            campaignSensorRepository.clearCache()
            surveyRepository.clearSurveys()
            triggerRepository.clearTriggers()
        }

        return profileResult
    }

    override suspend fun createProfileIfNotExists(
        email: String,
        campaignId: Int?
    ): Result<Unit> {
        val uuid = getCurrentUuid()
            ?: return Result.Error(AppError.Unknown("User not logged in"))
        return profileService.createProfileIfNotExists(uuid, email, campaignId)
    }
}
