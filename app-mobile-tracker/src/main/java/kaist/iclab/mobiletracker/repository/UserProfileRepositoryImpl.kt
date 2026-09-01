package kaist.iclab.mobiletracker.repository

import kaist.iclab.mobiletracker.data.campaign.ProfileData
import kaist.iclab.mobiletracker.helpers.BLEHelper
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.services.ProfileService
import kaist.iclab.mobiletracker.services.WatchSurveyConfigPusher
import kaist.iclab.mobiletracker.utils.SensorTypeHelper
import kaist.iclab.mobiletracker.utils.SupabaseSessionHelper
import kaist.iclab.tracker.sensor.controller.BackgroundController
import kaist.iclab.tracker.sensor.controller.ControllerState
import kaist.iclab.tracker.sensor.phone.TimingSensor
import kaist.iclab.tracker.storage.core.StateStorage
import kaist.iclab.tracker.trigger.engine.TriggerEngine
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
    private val timingSensorConfigStorage: StateStorage<TimingSensor.Config>,
    private val webAppRepository: WebAppRepository,
    private val watchSurveyConfigPusher: WatchSurveyConfigPusher,
    private val triggerEngine: TriggerEngine,
    private val backgroundController: BackgroundController,
    private val bleHelper: BLEHelper
) : UserProfileRepository {

    companion object {
        private const val TAG = "UserProfileRepo"
    }

    private val _profile = MutableStateFlow(
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
        timingSensorConfigStorage.set(TimingSensor.Config())
        webAppRepository.clearWebApps()
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

        val uuid = getCurrentUuid()
            ?: return Result.Error(AppError.Unknown("User not logged in"))

        // 1. Fetch the latest profile WITHOUT publishing it yet. Publishing the
        //    campaign ID immediately would trigger navigation away from the
        //    onboarding screen (NavGraph observes profileFlow.campaignId), which
        //    cancels this scope before the sensors/surveys below finish loading.
        val profileResult = profileService.getProfileByUuid(uuid)
        if (profileResult is Result.Error) return profileResult

        val profile = profileResult.getOrNull()
        val campaignId = profile?.campaignId

        // 2. If we have a campaign, fetch sensors, surveys, and triggers; otherwise clear caches.
        if (campaignId != null) {
            campaignSensorRepository.fetchActiveSensors(campaignId.toLong())
            surveyRepository.fetchAndPersistSurveys(campaignId)
            triggerRepository.fetchAndPersistTriggers(campaignId)
            webAppRepository.fetchAndPersistWebApps(campaignId)

            // 3. Apply TimingSensor's config from the campaign_table row we just fetched above
            //    (name = TimingSensor.CAMPAIGN_TABLE_NAME) — same generic per-sensor config
            //    column every sensor will eventually read from, not a dedicated table.
            val timingConfigJson =
                campaignSensorRepository.getSensorConfig(TimingSensor.CAMPAIGN_TABLE_NAME)
            timingSensorConfigStorage.set(
                if (timingConfigJson != null) {
                    try {
                        TimingSensor.Config.fromJson(timingConfigJson.toString())
                    } catch (e: Exception) {
                        TimingSensor.Config()
                    }
                } else {
                    TimingSensor.Config()
                }
            )

            // 4. Load triggers into the phone-local trigger engine — the engine now lives
            //    entirely on the phone, so there's no more BLE trigger/condition push to the
            //    watch at all.
            triggerEngine.loadTriggers(triggerRepository.getParsedTriggers())

            // 5. Push watch survey (question content) configs to the watch via BLE — the watch
            //    still needs these to render WatchSurveyActivity once told (over BLE) which
            //    survey to launch, but no longer needs trigger/condition data.
            val surveys = surveyRepository.getCachedSurveys().configs
            watchSurveyConfigPusher.pushToWatch(surveys)

            bleHelper.sendActiveSensorConfig(
                SensorTypeHelper.activeWatchSensorIds(
                    campaignSensorRepository
                )
            )
        } else {
            campaignSensorRepository.clearCache()
            surveyRepository.clearSurveys()
            triggerRepository.clearTriggers()
            timingSensorConfigStorage.set(TimingSensor.Config())
            webAppRepository.clearWebApps()
        }

        // 3. Publish the profile last, so observers (e.g. navigation reacting to
        //    campaignId) only react once the study config is fully in place.
        _profile.value = profile
        profile?.let { persistentStorage.set(it) }

        return profileResult
    }

    override suspend fun leaveCampaign(): Result<Unit> {
        val uuid = getCurrentUuid()
            ?: return Result.Error(AppError.Unknown("User not logged in"))
        return profileService.leaveCampaign(uuid)
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
