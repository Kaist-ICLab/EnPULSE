package kaist.iclab.mobiletracker.services.supabase

import kaist.iclab.mobiletracker.config.AppConfig
import kaist.iclab.mobiletracker.data.sensors.phone.WifiScanSensorData
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.repository.Result

/**
 * Service for handling WiFi sensor data operations with Supabase
 */
class WifiSensorService(
    supabaseHelper: SupabaseHelper
) : BaseSupabaseService<WifiScanSensorData>(
    supabaseHelper = supabaseHelper,
    tableName = AppConfig.SupabaseTables.WIFI_SCAN_SENSOR,
    sensorName = "Wifi"
) {

    override fun prepareData(data: WifiScanSensorData): WifiScanSensorData {
        // Don't override UUID - it should already be set to user UUID
        return data
    }

    suspend fun insertWifiSensorData(data: WifiScanSensorData): Result<Unit> {
        return upsertToSupabase(prepareData(data))
    }
    
    suspend fun insertWifiSensorDataBatch(dataList: List<WifiScanSensorData>): Result<Unit> {
        val preparedList = dataList.map { prepareData(it) }
        return upsertBatchToSupabase(preparedList)
    }
}
