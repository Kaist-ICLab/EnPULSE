package kaist.iclab.mobiletracker.services.upload.handlers.phone

import kaist.iclab.mobiletracker.data.DeviceType
import kaist.iclab.mobiletracker.db.dao.common.LocationDao
import kaist.iclab.mobiletracker.db.mapper.PhoneLocationMapper
import kaist.iclab.mobiletracker.repository.Result
import kaist.iclab.mobiletracker.services.supabase.LocationSensorService
import kaist.iclab.mobiletracker.services.upload.handlers.SensorUploadHandler

/**
 * Upload handler for Location sensor data (phone only).
 */
class LocationUploadHandler(
    private val dao: LocationDao,
    private val service: LocationSensorService
) : SensorUploadHandler {
    override val sensorId = "Location"

    override suspend fun hasDataToUpload(lastUploadTimestamp: Long): Boolean {
        val entities = dao.getDataAfterTimestampByDeviceType(lastUploadTimestamp, DeviceType.PHONE.value)
        return entities.isNotEmpty()
    }

    override suspend fun uploadData(userUuid: String, lastUploadTimestamp: Long): Result<Long> {
        return try {
            val entities = dao.getDataAfterTimestampByDeviceType(lastUploadTimestamp, DeviceType.PHONE.value)
            if (entities.isEmpty()) {
                return Result.Error(IllegalStateException("No new data available to upload"))
            }

            val supabaseDataList = entities.map { PhoneLocationMapper.map(it, userUuid) }
            val result = service.insertLocationSensorDataBatch(supabaseDataList)

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
