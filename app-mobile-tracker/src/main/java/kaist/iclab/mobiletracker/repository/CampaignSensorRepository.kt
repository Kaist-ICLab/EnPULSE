package kaist.iclab.mobiletracker.repository

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kaist.iclab.mobiletracker.data.sensors.CampaignSensorList
import kaist.iclab.mobiletracker.data.sensors.CampaignTableData
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.storage.CampaignSensorConfigStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.JsonElement

/**
 * Repository for fetching and caching sensors allowed for a specific campaign.
 */
interface CampaignSensorRepository {
    /**
     * Flow of active sensors (campaign tables) for the currently joined campaign.
     */
    val activeSensorsFlow: StateFlow<List<CampaignTableData>>

    /**
     * Fetch active sensors for a specific campaign ID from Supabase.
     */
    suspend fun fetchActiveSensors(campaignId: Long): Result<List<CampaignTableData>>

    /**
     * Get the cached active sensors synchronously.
     */
    fun getActiveSensors(): List<CampaignTableData>

    /**
     * Get the cached `campaign_table.config` JSON for a specific sensor by its campaign-table
     * `name` (e.g. `"timing_sensor"`), or null if that sensor has no row / no config for the
     * current campaign.
     */
    fun getSensorConfig(sensorName: String): JsonElement?

    /**
     * Clear the cache (e.g., on logout).
     */
    fun clearCache()
}

class CampaignSensorRepositoryImpl(
    private val supabaseHelper: SupabaseHelper,
    private val persistentStorage: CampaignSensorConfigStorage
) : CampaignSensorRepository {

    companion object {
        private const val TAG = "CampaignSensorRepo"
        private const val TABLE_NAME = "campaign_table"
    }

    private val _activeSensorsFlow =
        MutableStateFlow(persistentStorage.get().sensors)
    override val activeSensorsFlow = _activeSensorsFlow.asStateFlow()

    override suspend fun fetchActiveSensors(campaignId: Long): Result<List<CampaignTableData>> {
        return ErrorClassifier.runClassified(TAG, "fetchActiveSensors") {
            val supabaseClient = supabaseHelper.supabaseClient
            val tables = supabaseClient.from(TABLE_NAME)
                .select(columns = Columns.ALL) {
                    filter {
                        eq("campaign_id", campaignId)
                    }
                }
                .decodeList<CampaignTableData>()

            // Update in-memory cache
            _activeSensorsFlow.value = tables
            // Persist to local storage
            persistentStorage.set(CampaignSensorList(tables))
            tables
        }
    }

    override fun getActiveSensors(): List<CampaignTableData> {
        return _activeSensorsFlow.value
    }

    override fun getSensorConfig(sensorName: String): JsonElement? {
        return _activeSensorsFlow.value.find { it.name == sensorName }?.config
    }

    override fun clearCache() {
        _activeSensorsFlow.value = emptyList()
        persistentStorage.set(CampaignSensorList())
    }
}
