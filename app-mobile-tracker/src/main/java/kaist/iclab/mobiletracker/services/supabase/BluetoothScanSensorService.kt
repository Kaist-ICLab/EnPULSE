package kaist.iclab.mobiletracker.services.supabase

import kaist.iclab.mobiletracker.config.AppConfig
import kaist.iclab.mobiletracker.data.sensors.phone.BluetoothScanSensorData
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.repository.Result

/**
 * Service for handling Bluetooth scan sensor data operations with Supabase
 */
class BluetoothScanSensorService(
    supabaseHelper: SupabaseHelper
) : BaseSupabaseService<BluetoothScanSensorData>(
    supabaseHelper = supabaseHelper,
    tableName = AppConfig.SupabaseTables.BLUETOOTH_SCAN_SENSOR,
    sensorName = "Bluetooth Scan"
) {
    
    override fun prepareData(data: BluetoothScanSensorData): BluetoothScanSensorData {
        // Don't override UUID - it should already be set to user UUID
        return data
    }
    
    suspend fun insertBluetoothScanSensorData(data: BluetoothScanSensorData): Result<Unit> {
        return upsertToSupabase(prepareData(data))
    }
    
    suspend fun insertBluetoothScanSensorDataBatch(dataList: List<BluetoothScanSensorData>): Result<Unit> {
        val preparedList = dataList.map { prepareData(it) }
        return upsertBatchToSupabase(preparedList)
    }
}

