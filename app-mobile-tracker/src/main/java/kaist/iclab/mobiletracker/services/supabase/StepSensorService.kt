package kaist.iclab.mobiletracker.services.supabase

import kaist.iclab.mobiletracker.config.AppConfig
import kaist.iclab.mobiletracker.data.sensors.phone.StepSensorData
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.repository.Result

/**
 * Service for handling step sensor data operations with Supabase
 */
class StepSensorService(
    supabaseHelper: SupabaseHelper
) : BaseSupabaseService<StepSensorData>(
    supabaseHelper = supabaseHelper,
    tableName = AppConfig.SupabaseTables.STEP_SENSOR,
    sensorName = "Step"
) {

    override fun prepareData(data: StepSensorData): StepSensorData {
        // Don't override UUID - it should already be set to user UUID
        return data
    }

    suspend fun insertStepSensorData(data: StepSensorData): Result<Unit> {
        return upsertToSupabase(prepareData(data))
    }

    suspend fun insertStepSensorDataBatch(dataList: List<StepSensorData>): Result<Unit> {
        val preparedList = dataList.map { prepareData(it) }
        return upsertBatchToSupabase(preparedList)
    }
}
