package kaist.iclab.mobiletracker.services.supabase

import kaist.iclab.mobiletracker.config.AppConfig
import kaist.iclab.mobiletracker.data.sensors.phone.MediaSensorData
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.repository.Result

/**
 * Service for handling media sensor data operations with Supabase
 */
class MediaSensorService(
    supabaseHelper: SupabaseHelper
) : BaseSupabaseService<MediaSensorData>(
    supabaseHelper = supabaseHelper,
    tableName = AppConfig.SupabaseTables.MEDIA_SENSOR,
    sensorName = "Media"
) {

    override fun prepareData(data: MediaSensorData): MediaSensorData {
        // Don't override UUID - it should already be set to user UUID
        return data
    }

    suspend fun insertMediaSensorData(data: MediaSensorData): Result<Unit> {
        return upsertToSupabase(prepareData(data))
    }

    suspend fun insertMediaSensorDataBatch(dataList: List<MediaSensorData>): Result<Unit> {
        val preparedList = dataList.map { prepareData(it) }
        return upsertBatchToSupabase(preparedList)
    }
}
