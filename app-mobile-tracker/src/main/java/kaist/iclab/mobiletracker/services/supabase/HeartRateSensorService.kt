package kaist.iclab.mobiletracker.services.supabase

import kaist.iclab.mobiletracker.config.AppConfig
import kaist.iclab.mobiletracker.data.sensors.watch.HeartRateSensorData
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.repository.Result

/**
 * Service for handling heart rate sensor data operations with Supabase
 */
class HeartRateSensorService(
    supabaseHelper: SupabaseHelper
) : BaseSupabaseService<HeartRateSensorData>(
    supabaseHelper = supabaseHelper,
    tableName = AppConfig.SupabaseTables.HEART_RATE_SENSOR,
    sensorName = "heart rate"
) {
    
    override fun prepareData(data: HeartRateSensorData): HeartRateSensorData {
        // Don't override UUID - it should already be set to user UUID from mapper
        return data
    }
    
    suspend fun insertHeartRateSensorData(data: HeartRateSensorData): Result<Unit> {
        return upsertToSupabase(prepareData(data))
    }
    
    suspend fun insertHeartRateSensorDataBatch(dataList: List<HeartRateSensorData>): Result<Unit> {
        val preparedList = dataList.map { prepareData(it) }
        return upsertBatchToSupabase(preparedList)
    }
}

