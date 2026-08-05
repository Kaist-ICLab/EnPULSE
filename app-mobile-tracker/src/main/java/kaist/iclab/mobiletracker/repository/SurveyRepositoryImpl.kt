package kaist.iclab.mobiletracker.repository

import android.util.Log
import kaist.iclab.mobiletracker.services.SurveyService
import kaist.iclab.mobiletracker.storage.CouchbaseSurveyConfigStorage
import kaist.iclab.mobiletracker.utils.SurveyConfigConverter
import kaist.iclab.tracker.sensor.galaxywatch.MicroEmaSensor
import kaist.iclab.tracker.sensor.microema.MicroEmaBuilder
import kaist.iclab.tracker.sensor.phone.SurveySensor
import kaist.iclab.tracker.sensor.survey.config.SurveyConfigList
import kaist.iclab.tracker.storage.core.StateStorage
import kaist.iclab.tracker.storage.core.SurveyScheduleStorage
import kotlinx.coroutines.flow.StateFlow

/**
 * Implementation of SurveyRepository.
 * Manages survey configurations from Supabase and persists them to Couchbase.
 *
 * Note: Watch trigger rules are no longer created here. Dynamic trigger
 * configurations are fetched separately and pushed to the watch via
 * [TriggerConfigPusher] in [UserProfileRepositoryImpl.syncFullStudyConfig].
 */
class SurveyRepositoryImpl(
    private val surveyService: SurveyService,
    private val persistentStorage: CouchbaseSurveyConfigStorage,
    private val phoneSensorConfigStorage: StateStorage<SurveySensor.Config>,
    private val microEmaConfigStorage: StateStorage<MicroEmaSensor.Config>,
    private val scheduleStorage: SurveyScheduleStorage
) : SurveyRepository {
    companion object {
        private const val TAG = "SurveyRepo"
    }

    override val surveysFlow: StateFlow<SurveyConfigList>
        get() = persistentStorage.stateFlow

    override fun getCachedSurveys(): SurveyConfigList = persistentStorage.get()

    override suspend fun fetchAndPersistSurveys(campaignId: Int): Result<Int> {
        return ErrorClassifier.runClassified(TAG, "fetch and persist surveys") {
            when (val result = surveyService.getCampaignSurveys(campaignId)) {
                is Result.Success -> {
                    val configs = result.data

                    if (configs.isNotEmpty()) {
                        // 1. Persist all to Couchbase
                        persistentStorage.set(SurveyConfigList(configs))

                        // 2. Partition and apply configs
                        applyConfigs(configs)
                    } else {
                        // Clear old surveys when new campaign has no surveys
                        clearSurveys()
                    }

                    configs.size
                }

                is Result.Error -> {
                    throw result.exception
                }
            }
        }
    }

    private fun applyConfigs(configs: List<kaist.iclab.tracker.sensor.survey.config.SurveyConfig>) {

        // 1. Filter and apply Phone Surveys
        val phoneConfigs = configs.filter { it.deviceType == 0 }
        val phoneSensorConfig = SurveyConfigConverter.toSurveySensorConfig(phoneConfigs)
        phoneSensorConfigStorage.set(phoneSensorConfig)

        // 2. Filter and apply Watch (MicroEMA) Surveys
        // Build watch EMA configs for the MicroEmaSensor (response listening only)
        val watchConfigs = configs.filter { it.deviceType == 1 }

        val validWatchConfigs = watchConfigs.filter { it.expireAfterMs != null }
        if (watchConfigs.size > validWatchConfigs.size) {
            Log.w(
                TAG,
                "${watchConfigs.size - validWatchConfigs.size} watch surveys were skipped because 'expire_after_ms' is null"
            )
        }

        if (validWatchConfigs.isNotEmpty()) {
            val watchEmaConfigs = validWatchConfigs.associate { survey ->
                val watchConfig = MicroEmaBuilder.build(survey)
                watchConfig.surveyId to watchConfig
            }

            // Store watch survey configs for response mapping (no trigger rules needed)
            microEmaConfigStorage.set(
                MicroEmaSensor.Config(
                    watchSurveyConfigs = watchEmaConfigs
                )
            )
        }
    }

    override fun getSensorConfig(): SurveySensor.Config {
        val savedConfigs = persistentStorage.get().configs
        return if (savedConfigs.isNotEmpty()) {
            SurveyConfigConverter.toSurveySensorConfig(savedConfigs.filter { it.deviceType == 0 })
        } else {
            SurveySensor.Config(survey = emptyMap())
        }
    }

    override fun clearSurveys() {
        persistentStorage.set(SurveyConfigList())
        phoneSensorConfigStorage.set(SurveySensor.Config(survey = emptyMap()))
        microEmaConfigStorage.set(MicroEmaSensor.Config())
        // Clear pending survey schedules/notifications from old configuration
        scheduleStorage.resetSchedule()
    }
}
