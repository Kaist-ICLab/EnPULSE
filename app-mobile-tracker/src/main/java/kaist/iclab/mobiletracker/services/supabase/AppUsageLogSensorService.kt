package kaist.iclab.mobiletracker.services.supabase

import kaist.iclab.mobiletracker.config.AppConfig
import kaist.iclab.mobiletracker.data.sensors.phone.AppUsageLogSensorData
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.repository.Result

/**
 * Service for handling app usage log sensor data operations with Supabase
 */
class AppUsageLogSensorService(
    supabaseHelper: SupabaseHelper
) : BaseSupabaseService<AppUsageLogSensorData>(
    supabaseHelper = supabaseHelper,
    tableName = AppConfig.SupabaseTables.APP_USAGE_LOG_SENSOR,
    sensorName = "App Usage Log"
) {

    override fun prepareData(data: AppUsageLogSensorData): AppUsageLogSensorData {
        // Don't override UUID - it should already be set to user UUID
        return data
    }

    suspend fun insertAppUsageLogSensorData(data: AppUsageLogSensorData): Result<Unit> {
        return upsertToSupabase(prepareData(data))
    }

    suspend fun insertAppUsageLogSensorDataBatch(dataList: List<AppUsageLogSensorData>): Result<Unit> {
        val preparedList = dataList.map { prepareData(it) }
        return upsertBatchToSupabase(preparedList)
    }
}
