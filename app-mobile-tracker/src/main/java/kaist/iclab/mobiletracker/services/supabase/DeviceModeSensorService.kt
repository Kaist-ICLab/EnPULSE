package kaist.iclab.mobiletracker.services.supabase

import kaist.iclab.mobiletracker.config.AppConfig
import kaist.iclab.mobiletracker.data.sensors.phone.DeviceModeSensorData
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.repository.Result

/**
 * Service for handling Device Mode sensor data operations with Supabase
 */
class DeviceModeSensorService(
    supabaseHelper: SupabaseHelper
) : BaseSupabaseService<DeviceModeSensorData>(
    supabaseHelper = supabaseHelper,
    tableName = AppConfig.SupabaseTables.DEVICE_MODE_SENSOR,
    sensorName = "Device Mode"
) {

    override fun prepareData(data: DeviceModeSensorData): DeviceModeSensorData {
        // Don't override UUID - it should already be set to user UUID
        return data
    }

    suspend fun insertDeviceModeSensorData(data: DeviceModeSensorData): Result<Unit> {
        return upsertToSupabase(prepareData(data))
    }

    suspend fun insertDeviceModeSensorDataBatch(dataList: List<DeviceModeSensorData>): Result<Unit> {
        val preparedList = dataList.map { prepareData(it) }
        return upsertBatchToSupabase(preparedList)
    }
}
