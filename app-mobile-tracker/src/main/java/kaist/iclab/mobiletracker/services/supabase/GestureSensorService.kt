package kaist.iclab.mobiletracker.services.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kaist.iclab.mobiletracker.config.AppConfig.SupabaseTables.GESTURE_SENSOR
import kaist.iclab.mobiletracker.data.sensors.watch.GestureSensorData

class GestureSensorService(private val supabaseClient: SupabaseClient) {
    private val tableName = GESTURE_SENSOR

    suspend fun insertGestureSensorDataBatch(sensorDataList: List<GestureSensorData>): Result<Unit> {
        return try {
            if (sensorDataList.isNotEmpty()) {
                supabaseClient.postgrest[tableName].insert(sensorDataList)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
