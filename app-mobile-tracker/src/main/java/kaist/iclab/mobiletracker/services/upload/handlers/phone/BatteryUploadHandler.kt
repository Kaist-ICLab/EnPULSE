package kaist.iclab.mobiletracker.services.upload.handlers.phone

import kaist.iclab.mobiletracker.db.dao.phone.BatteryDao
import kaist.iclab.mobiletracker.db.mapper.BatteryMapper
import kaist.iclab.mobiletracker.repository.Result
import kaist.iclab.mobiletracker.services.supabase.BatterySensorService
import kaist.iclab.mobiletracker.services.upload.handlers.SensorUploadHandler

/**
 * Upload handler for Battery sensor data.
 */
class BatteryUploadHandler(
    private val dao: BatteryDao,
    private val service: BatterySensorService
) : SensorUploadHandler {
    override val sensorId = "Battery"

    override suspend fun hasDataToUpload(lastUploadTimestamp: Long): Boolean {
        val entities = dao.getDataAfterTimestamp(lastUploadTimestamp)
        return entities.isNotEmpty()
    }

    override suspend fun uploadData(userUuid: String, lastUploadTimestamp: Long): Result<Long> {
        return try {
            val entities = dao.getDataAfterTimestamp(lastUploadTimestamp)
            if (entities.isEmpty()) {
                return Result.Error(IllegalStateException("No new data available to upload"))
            }

            val supabaseDataList = entities.map { BatteryMapper.map(it, userUuid) }
            val result = service.insertBatterySensorDataBatch(supabaseDataList)

            if (result is Result.Success) {
                val maxTimestamp = entities.maxOf { it.timestamp }
                Result.Success(maxTimestamp)
            } else {
                result as Result.Error
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
