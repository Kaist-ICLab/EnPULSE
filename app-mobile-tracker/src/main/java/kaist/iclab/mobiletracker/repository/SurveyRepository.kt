package kaist.iclab.mobiletracker.repository

import kaist.iclab.tracker.sensor.phone.SurveySensor
import kaist.iclab.tracker.sensor.survey.config.SurveyConfigList
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository for managing survey configurations.
 * Centralizes survey data access from remote (Supabase) and local (Couchbase) sources.
 */
interface SurveyRepository {

    /**
     * Observable flow of locally persisted survey configurations
     */
    val surveysFlow: StateFlow<SurveyConfigList>

    /**
     * Fetch surveys for a campaign from Supabase and persist locally.
     * Also applies the configuration to the in-memory SurveySensor.
     *
     * @param campaignId The campaign ID to fetch surveys for
     * @return Result containing the count of surveys fetched, or error
     */
    suspend fun fetchAndPersistSurveys(campaignId: Int): Result<Int>

    /**
     * Get the current SurveySensor configuration from persisted storage.
     * Used for priming the sensor at startup.
     */
    fun getSensorConfig(): SurveySensor.Config

    /**
     * Clear all locally persisted survey configurations.
     */
    fun clearSurveys()
}
