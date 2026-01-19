package kaist.iclab.mobiletracker.services.upload.handlers.phone

import kaist.iclab.mobiletracker.db.dao.phone.MediaDao
import kaist.iclab.mobiletracker.db.mapper.MediaMapper
import kaist.iclab.mobiletracker.repository.Result
import kaist.iclab.mobiletracker.services.supabase.MediaSensorService
import kaist.iclab.mobiletracker.services.upload.handlers.SensorUploadHandler

/**
 * Upload handler for Media sensor data.
 */
class MediaUploadHandler(
    private val dao: MediaDao,
    private val service: MediaSensorService
) : SensorUploadHandler {
    override val sensorId = "Media"

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

            val supabaseDataList = entities.map { MediaMapper.map(it, userUuid) }
            val result = service.insertMediaSensorDataBatch(supabaseDataList)

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
