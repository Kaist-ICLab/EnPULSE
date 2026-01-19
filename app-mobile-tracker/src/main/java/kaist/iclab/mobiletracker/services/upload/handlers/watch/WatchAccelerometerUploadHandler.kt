package kaist.iclab.mobiletracker.services.upload.handlers.watch

import kaist.iclab.mobiletracker.db.dao.watch.WatchAccelerometerDao
import kaist.iclab.mobiletracker.db.mapper.AccelerometerMapper
import kaist.iclab.mobiletracker.repository.Result
import kaist.iclab.mobiletracker.services.supabase.AccelerometerSensorService
import kaist.iclab.mobiletracker.services.upload.handlers.SensorUploadHandler

/**
 * Upload handler for Watch Accelerometer sensor data.
 */
class WatchAccelerometerUploadHandler(
    private val dao: WatchAccelerometerDao,
    private val service: AccelerometerSensorService
) : SensorUploadHandler {
    override val sensorId = "WatchAccelerometer"

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

            val supabaseDataList = entities.map { AccelerometerMapper.map(it, userUuid) }
            val result = service.insertAccelerometerSensorDataBatch(supabaseDataList)

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
