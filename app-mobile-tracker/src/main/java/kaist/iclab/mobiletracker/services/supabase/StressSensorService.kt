package kaist.iclab.mobiletracker.services.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kaist.iclab.mobiletracker.data.sensors.watch.StressSensorData

class StressSensorService(private val supabaseClient: SupabaseClient) {
    private val tableName = "sensor_stress"

    suspend fun insertStressSensorDataBatch(sensorDataList: List<StressSensorData>): Result<Unit> {
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
