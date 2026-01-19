package kaist.iclab.mobiletracker.services.supabase

import kaist.iclab.mobiletracker.config.AppConfig
import kaist.iclab.mobiletracker.data.sensors.watch.SkinTemperatureSensorData
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.repository.Result

/**
 * Service for handling skin temperature sensor data operations with Supabase
 */
class SkinTemperatureSensorService(
    supabaseHelper: SupabaseHelper
) : BaseSupabaseService<SkinTemperatureSensorData>(
    supabaseHelper = supabaseHelper,
    tableName = AppConfig.SupabaseTables.SKIN_TEMPERATURE_SENSOR,
    sensorName = "skin temperature"
) {
    
    override fun prepareData(data: SkinTemperatureSensorData): SkinTemperatureSensorData {
        // Don't override UUID - it should already be set to user UUID from mapper
        return data
    }
    
    suspend fun insertSkinTemperatureSensorData(data: SkinTemperatureSensorData): Result<Unit> {
        return upsertToSupabase(prepareData(data))
    }
    
    suspend fun insertSkinTemperatureSensorDataBatch(dataList: List<SkinTemperatureSensorData>): Result<Unit> {
        val preparedList = dataList.map { prepareData(it) }
        return upsertBatchToSupabase(preparedList)
    }
}

