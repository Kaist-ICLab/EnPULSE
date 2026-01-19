package kaist.iclab.mobiletracker.services.supabase

import kaist.iclab.mobiletracker.config.AppConfig
import kaist.iclab.mobiletracker.data.sensors.phone.CallLogSensorData
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.repository.Result

/**
 * Service for handling call log sensor data operations with Supabase
 */
class CallLogSensorService(
    supabaseHelper: SupabaseHelper
) : BaseSupabaseService<CallLogSensorData>(
    supabaseHelper = supabaseHelper,
    tableName = AppConfig.SupabaseTables.CALL_LOG_SENSOR,
    sensorName = "Call Log"
) {
    
    override fun prepareData(data: CallLogSensorData): CallLogSensorData {
        // Don't override UUID - it should already be set to user UUID
        return data
    }
    
    suspend fun insertCallLogSensorData(data: CallLogSensorData): Result<Unit> {
        return upsertToSupabase(prepareData(data))
    }
    
    suspend fun insertCallLogSensorDataBatch(dataList: List<CallLogSensorData>): Result<Unit> {
        val preparedList = dataList.map { prepareData(it) }
        return upsertBatchToSupabase(preparedList)
    }
}
