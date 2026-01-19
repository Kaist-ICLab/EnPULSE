package kaist.iclab.mobiletracker.services.supabase

import kaist.iclab.mobiletracker.config.AppConfig
import kaist.iclab.mobiletracker.data.sensors.phone.NotificationSensorData
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.repository.Result

/**
 * Service for handling notification sensor data operations with Supabase
 */
class NotificationSensorService(
    supabaseHelper: SupabaseHelper
) : BaseSupabaseService<NotificationSensorData>(
    supabaseHelper = supabaseHelper,
    tableName = AppConfig.SupabaseTables.NOTIFICATION_SENSOR,
    sensorName = "Notification"
) {

    override fun prepareData(data: NotificationSensorData): NotificationSensorData {
        // Don't override UUID - it should already be set to user UUID
        return data
    }

    suspend fun insertNotificationSensorData(data: NotificationSensorData): Result<Unit> {
        return upsertToSupabase(prepareData(data))
    }

    suspend fun insertNotificationSensorDataBatch(dataList: List<NotificationSensorData>): Result<Unit> {
        val preparedList = dataList.map { prepareData(it) }
        return upsertBatchToSupabase(preparedList)
    }
}
