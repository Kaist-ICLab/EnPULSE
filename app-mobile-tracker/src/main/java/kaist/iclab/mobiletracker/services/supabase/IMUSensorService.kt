package kaist.iclab.mobiletracker.services.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kaist.iclab.mobiletracker.data.sensors.watch.IMUSensorData

class IMUSensorService(private val supabaseClient: SupabaseClient) {
    private val tableName = "sensor_imu"

    suspend fun insertIMUSensorDataBatch(sensorDataList: List<IMUSensorData>): Result<Unit> {
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
