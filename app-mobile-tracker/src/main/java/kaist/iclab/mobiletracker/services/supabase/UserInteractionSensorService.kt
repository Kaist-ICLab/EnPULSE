package kaist.iclab.mobiletracker.services.supabase

import kaist.iclab.mobiletracker.config.AppConfig
import kaist.iclab.mobiletracker.data.sensors.phone.UserInteractionSensorData
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.repository.Result

/**
 * Service for handling user interaction sensor data operations with Supabase
 */
class UserInteractionSensorService(
    supabaseHelper: SupabaseHelper
) : BaseSupabaseService<UserInteractionSensorData>(
    supabaseHelper = supabaseHelper,
    tableName = AppConfig.SupabaseTables.USER_INTERACTION_SENSOR,
    sensorName = "User Interaction"
) {

    override fun prepareData(data: UserInteractionSensorData): UserInteractionSensorData {
        // Don't override UUID - it should already be set to user UUID
        return data
    }

    suspend fun insertUserInteractionSensorData(data: UserInteractionSensorData): Result<Unit> {
        return upsertToSupabase(prepareData(data))
    }

    suspend fun insertUserInteractionSensorDataBatch(dataList: List<UserInteractionSensorData>): Result<Unit> {
        val preparedList = dataList.map { prepareData(it) }
        return upsertBatchToSupabase(preparedList)
    }
}
