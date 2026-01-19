package kaist.iclab.mobiletracker.services.supabase

import kaist.iclab.mobiletracker.config.AppConfig
import kaist.iclab.mobiletracker.data.sensors.phone.AppListChangeSensorData
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.repository.Result

/**
 * Service for handling app list change sensor data operations with Supabase
 */
class AppListChangeSensorService(
    supabaseHelper: SupabaseHelper
) : BaseSupabaseService<AppListChangeSensorData>(
    supabaseHelper = supabaseHelper,
    tableName = AppConfig.SupabaseTables.APP_LIST_CHANGE_SENSOR,
    sensorName = "App List Change"
) {
    
    override fun prepareData(data: AppListChangeSensorData): AppListChangeSensorData {
        // Don't override UUID - it should already be set to user UUID
        return data
    }
    
    suspend fun insertAppListChangeSensorData(data: AppListChangeSensorData): Result<Unit> {
        return upsertToSupabase(prepareData(data))
    }
    
    suspend fun insertAppListChangeSensorDataBatch(dataList: List<AppListChangeSensorData>): Result<Unit> {
        val preparedList = dataList.map { prepareData(it) }
        return upsertBatchToSupabase(preparedList)
    }
}
