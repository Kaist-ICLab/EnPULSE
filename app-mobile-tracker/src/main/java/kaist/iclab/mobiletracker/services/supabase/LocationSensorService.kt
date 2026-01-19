package kaist.iclab.mobiletracker.services.supabase

import kaist.iclab.mobiletracker.config.AppConfig
import kaist.iclab.mobiletracker.data.sensors.common.LocationSensorData
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.repository.Result

/**
 * Service for handling location sensor data operations with Supabase
 */
class LocationSensorService(
    supabaseHelper: SupabaseHelper
) : BaseSupabaseService<LocationSensorData>(
    supabaseHelper = supabaseHelper,
    tableName = AppConfig.SupabaseTables.LOCATION_SENSOR,
    sensorName = "location"
) {
    
    override fun prepareData(data: LocationSensorData): LocationSensorData {
        // Don't override UUID - it should already be set to user UUID from mapper
        return data
    }
    
    suspend fun insertLocationSensorData(data: LocationSensorData): Result<Unit> {
        return upsertToSupabase(prepareData(data))
    }
    
    suspend fun insertLocationSensorDataBatch(dataList: List<LocationSensorData>): Result<Unit> {
        val preparedList = dataList.map { prepareData(it) }
        return upsertBatchToSupabase(preparedList)
    }
}

