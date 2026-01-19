package kaist.iclab.mobiletracker.services.supabase

import kaist.iclab.mobiletracker.config.AppConfig
import kaist.iclab.mobiletracker.data.sensors.phone.ConnectivitySensorData
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.repository.Result

/**
 * Service for handling connectivity sensor data operations with Supabase
 */
class ConnectivitySensorService(
    supabaseHelper: SupabaseHelper
) : BaseSupabaseService<ConnectivitySensorData>(
    supabaseHelper = supabaseHelper,
    tableName = AppConfig.SupabaseTables.CONNECTIVITY_SENSOR,
    sensorName = "Connectivity"
) {

    override fun prepareData(data: ConnectivitySensorData): ConnectivitySensorData {
        // Don't override UUID - it should already be set to user UUID
        return data
    }

    suspend fun insertConnectivitySensorData(data: ConnectivitySensorData): Result<Unit> {
        return upsertToSupabase(prepareData(data))
    }

    suspend fun insertConnectivitySensorDataBatch(dataList: List<ConnectivitySensorData>): Result<Unit> {
        val preparedList = dataList.map { prepareData(it) }
        return upsertBatchToSupabase(preparedList)
    }
}
