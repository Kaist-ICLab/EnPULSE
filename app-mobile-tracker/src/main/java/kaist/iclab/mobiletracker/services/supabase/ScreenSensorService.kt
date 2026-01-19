package kaist.iclab.mobiletracker.services.supabase

import kaist.iclab.mobiletracker.config.AppConfig
import kaist.iclab.mobiletracker.data.sensors.phone.ScreenSensorData
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.repository.Result

/**
 * Service for handling screen sensor data operations with Supabase
 */
class ScreenSensorService(
    supabaseHelper: SupabaseHelper
) : BaseSupabaseService<ScreenSensorData>(
    supabaseHelper = supabaseHelper,
    tableName = AppConfig.SupabaseTables.SCREEN_SENSOR,
    sensorName = "Screen"
) {
    
    override fun prepareData(data: ScreenSensorData): ScreenSensorData {
        // Don't override UUID - it should already be set to user UUID
        return data
    }
    
    suspend fun insertScreenSensorData(data: ScreenSensorData): Result<Unit> {
        return upsertToSupabase(prepareData(data))
    }
    
    suspend fun insertScreenSensorDataBatch(dataList: List<ScreenSensorData>): Result<Unit> {
        val preparedList = dataList.map { prepareData(it) }
        return upsertBatchToSupabase(preparedList)
    }
}

