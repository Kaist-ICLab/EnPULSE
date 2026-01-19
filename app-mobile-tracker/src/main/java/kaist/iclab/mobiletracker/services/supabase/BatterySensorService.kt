package kaist.iclab.mobiletracker.services.supabase

import kaist.iclab.mobiletracker.config.AppConfig
import kaist.iclab.mobiletracker.data.sensors.phone.BatterySensorData
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.repository.Result

/**
 * Service for handling battery sensor data operations with Supabase
 */
class BatterySensorService(
    supabaseHelper: SupabaseHelper
) : BaseSupabaseService<BatterySensorData>(
    supabaseHelper = supabaseHelper,
    tableName = AppConfig.SupabaseTables.BATTERY_SENSOR,
    sensorName = "Battery"
) {
    
    override fun prepareData(data: BatterySensorData): BatterySensorData {
        // Don't override UUID - it should already be set to user UUID
        return data
    }
    
    suspend fun insertBatterySensorData(data: BatterySensorData): Result<Unit> {
        return upsertToSupabase(prepareData(data))
    }
    
    suspend fun insertBatterySensorDataBatch(dataList: List<BatterySensorData>): Result<Unit> {
        val preparedList = dataList.map { prepareData(it) }
        return upsertBatchToSupabase(preparedList)
    }
}

