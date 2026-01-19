package kaist.iclab.mobiletracker.services.upload.handlers.phone

import kaist.iclab.mobiletracker.db.dao.phone.CallLogDao
import kaist.iclab.mobiletracker.db.mapper.CallLogMapper
import kaist.iclab.mobiletracker.repository.Result
import kaist.iclab.mobiletracker.services.supabase.CallLogSensorService
import kaist.iclab.mobiletracker.services.upload.handlers.SensorUploadHandler

/**
 * Upload handler for Call Log sensor data.
 */
class CallLogUploadHandler(
    private val dao: CallLogDao,
    private val service: CallLogSensorService
) : SensorUploadHandler {
    override val sensorId = "CallLog"

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

            val supabaseDataList = entities.map { CallLogMapper.map(it, userUuid) }
            val result = service.insertCallLogSensorDataBatch(supabaseDataList)

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
