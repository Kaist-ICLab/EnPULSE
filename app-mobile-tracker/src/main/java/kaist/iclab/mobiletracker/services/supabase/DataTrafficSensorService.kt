package kaist.iclab.mobiletracker.services.supabase

import kaist.iclab.mobiletracker.config.AppConfig
import kaist.iclab.mobiletracker.data.sensors.phone.DataTrafficSensorData
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.repository.Result

/**
 * Service for handling Data Traffic sensor data operations with Supabase
 */
class DataTrafficSensorService(
    supabaseHelper: SupabaseHelper
) : BaseSupabaseService<DataTrafficSensorData>(
    supabaseHelper = supabaseHelper,
    tableName = AppConfig.SupabaseTables.DATA_TRAFFIC_SENSOR,
    sensorName = "Data Traffic"
) {

    override fun prepareData(data: DataTrafficSensorData): DataTrafficSensorData {
        // Don't override UUID - it should already be set to user UUID
        return data
    }

    suspend fun insertDataTrafficSensorData(data: DataTrafficSensorData): Result<Unit> {
        return upsertToSupabase(prepareData(data))
    }

    suspend fun insertDataTrafficSensorDataBatch(dataList: List<DataTrafficSensorData>): Result<Unit> {
        val preparedList = dataList.map { prepareData(it) }
        return upsertBatchToSupabase(preparedList)
    }
}
