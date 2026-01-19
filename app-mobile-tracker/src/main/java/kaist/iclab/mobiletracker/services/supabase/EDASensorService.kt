package kaist.iclab.mobiletracker.services.supabase

import kaist.iclab.mobiletracker.config.AppConfig
import kaist.iclab.mobiletracker.data.sensors.watch.EDASensorData
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.repository.Result

/**
 * Service for handling EDA (Electrodermal Activity) sensor data operations with Supabase
 */
class EDASensorService(
    supabaseHelper: SupabaseHelper
) : BaseSupabaseService<EDASensorData>(
    supabaseHelper = supabaseHelper,
    tableName = AppConfig.SupabaseTables.EDA_SENSOR,
    sensorName = "EDA"
) {
    
    override fun prepareData(data: EDASensorData): EDASensorData {
        // Don't override UUID - it should already be set to user UUID from mapper
        return data
    }
    
    suspend fun insertEDASensorData(data: EDASensorData): Result<Unit> {
        return upsertToSupabase(prepareData(data))
    }
    
    suspend fun insertEDASensorDataBatch(dataList: List<EDASensorData>): Result<Unit> {
        val preparedList = dataList.map { prepareData(it) }
        return upsertBatchToSupabase(preparedList)
    }
}

