package kaist.iclab.mobiletracker.services.upload.handlers.phone

import kaist.iclab.mobiletracker.data.DeviceType
import kaist.iclab.mobiletracker.db.mapper.PhoneLocationMapper
import kaist.iclab.mobiletracker.db.obx.LocationStore
import kaist.iclab.mobiletracker.repository.ErrorClassifier
import kaist.iclab.mobiletracker.repository.Result
import kaist.iclab.mobiletracker.services.supabase.LocationSensorService
import kaist.iclab.mobiletracker.services.upload.handlers.SensorUploadHandler

/**
 * Upload handler for Location sensor data (phone only), backed by [LocationStore].
 */
class LocationUploadHandler(
    private val store: LocationStore,
    private val service: LocationSensorService
) : SensorUploadHandler {
    override val sensorId = "Location"

    override suspend fun hasDataToUpload(lastUploadTimestamp: Long): Boolean {
        return store.hasDataAfterByDeviceType(lastUploadTimestamp, DeviceType.PHONE.value)
    }

    override suspend fun uploadData(userUuid: String, lastUploadTimestamp: Long): Result<Long> {
        return ErrorClassifier.runClassified(sensorId, "upload $sensorId") {
            val batchSize = kaist.iclab.mobiletracker.Constants.Network.UPLOAD_BATCH_SIZE
            var currentMaxTimestamp = lastUploadTimestamp
            var uploadedAny = false

            while (true) {
                val entities = store.recordsAfterByDeviceType(
                    afterTimestamp = currentMaxTimestamp + 1,
                    isAscending = true,
                    limit = batchSize,
                    offset = 0,
                    deviceType = DeviceType.PHONE.value
                )

                if (entities.isEmpty()) break

                val supabaseDataList =
                    entities.map { entity -> PhoneLocationMapper.map(entity, userUuid) }
                service.insertLocationSensorDataBatch(supabaseDataList)
                    .getOrElse { error -> throw error }

                currentMaxTimestamp = entities.maxOf { entity -> entity.timestamp }
                uploadedAny = true

                if (entities.size < batchSize) break
            }

            if (!uploadedAny) {
                throw IllegalStateException("No new $sensorId data to upload")
            }

            currentMaxTimestamp
        }
    }

    override suspend fun pruneData(beforeTimestamp: Long) {
        store.removeBeforeByDeviceType(beforeTimestamp, DeviceType.PHONE.value)
    }

    override suspend fun getRecordCount(): Int {
        return store.countByDeviceType(DeviceType.PHONE.value)
    }

    override suspend fun getRecordsPaginated(limit: Int, offset: Int): List<Any> {
        return store.recordsAfterByDeviceType(0L, true, limit, offset, DeviceType.PHONE.value)
    }

    override fun getCsvHeader(): String {
        return "eventId,uuid,deviceType,received,timestamp,latitude,longitude,altitude,speed,accuracy"
    }

    override fun recordToCsvRow(record: Any): String {
        val entity = record as kaist.iclab.mobiletracker.db.entity.common.LocationEntity
        return "${entity.eventId},${entity.uuid},${entity.deviceType},${entity.received},${entity.timestamp},${entity.latitude},${entity.longitude},${entity.altitude},${entity.speed},${entity.accuracy}"
    }
}
