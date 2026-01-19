package kaist.iclab.mobiletracker.services.supabase

import kaist.iclab.mobiletracker.config.AppConfig
import kaist.iclab.mobiletracker.data.sensors.watch.AccelerometerSensorData
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.repository.Result

/**
 * Service for handling accelerometer sensor data operations with Supabase
 */
class AccelerometerSensorService(
    supabaseHelper: SupabaseHelper
) : BaseSupabaseService<AccelerometerSensorData>(
    supabaseHelper = supabaseHelper,
    tableName = AppConfig.SupabaseTables.ACCELEROMETER_SENSOR,
    sensorName = "accelerometer"
) {
    
    override fun prepareData(data: AccelerometerSensorData): AccelerometerSensorData {
        // Don't override UUID - it should already be set to user UUID from mapper
        return data
    }
    
    suspend fun insertAccelerometerSensorData(data: AccelerometerSensorData): Result<Unit> {
        return upsertToSupabase(prepareData(data))
    }
    
    suspend fun insertAccelerometerSensorDataBatch(dataList: List<AccelerometerSensorData>): Result<Unit> {
        val preparedList = dataList.map { prepareData(it) }
        return upsertBatchToSupabase(preparedList)
    }
}

