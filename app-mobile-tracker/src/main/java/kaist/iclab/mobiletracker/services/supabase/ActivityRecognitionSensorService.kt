package kaist.iclab.mobiletracker.services.supabase

import kaist.iclab.mobiletracker.config.AppConfig
import kaist.iclab.mobiletracker.data.sensors.phone.ActivityRecognitionSensorData
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.repository.Result

class ActivityRecognitionSensorService(
    supabaseHelper: SupabaseHelper
) : BaseSupabaseService<ActivityRecognitionSensorData>(
    supabaseHelper = supabaseHelper,
    tableName = AppConfig.SupabaseTables.ACTIVITY_RECOGNITION_SENSOR,
    sensorName = "Activity Recognition"
) {

    override fun prepareData(data: ActivityRecognitionSensorData): ActivityRecognitionSensorData {
        return data
    }

    suspend fun insertActivityRecognitionSensorData(data: ActivityRecognitionSensorData): Result<Unit> {
        return upsertToSupabase(prepareData(data))
    }

    suspend fun insertActivityRecognitionSensorDataBatch(dataList: List<ActivityRecognitionSensorData>): Result<Unit> {
        val preparedList = dataList.map { prepareData(it) }
        return upsertBatchToSupabase(preparedList)
    }
}
