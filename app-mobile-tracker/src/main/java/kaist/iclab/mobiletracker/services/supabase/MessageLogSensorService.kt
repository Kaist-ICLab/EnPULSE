package kaist.iclab.mobiletracker.services.supabase

import kaist.iclab.mobiletracker.config.AppConfig
import kaist.iclab.mobiletracker.data.sensors.phone.MessageLogSensorData
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.repository.Result

/**
 * Service for handling message log sensor data operations with Supabase
 */
class MessageLogSensorService(
    supabaseHelper: SupabaseHelper
) : BaseSupabaseService<MessageLogSensorData>(
    supabaseHelper = supabaseHelper,
    tableName = AppConfig.SupabaseTables.MESSAGE_LOG_SENSOR,
    sensorName = "Message Log"
) {

    override fun prepareData(data: MessageLogSensorData): MessageLogSensorData {
        // Don't override UUID - it should already be set to user UUID
        return data
    }

    suspend fun insertMessageLogSensorData(data: MessageLogSensorData): Result<Unit> {
        return upsertToSupabase(prepareData(data))
    }

    suspend fun insertMessageLogSensorDataBatch(dataList: List<MessageLogSensorData>): Result<Unit> {
        val preparedList = dataList.map { prepareData(it) }
        return upsertBatchToSupabase(preparedList)
    }
}
