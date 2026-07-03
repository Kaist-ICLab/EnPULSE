package kaist.iclab.mobiletracker.services.upload.handlers.phone

import kaist.iclab.mobiletracker.Constants
import kaist.iclab.mobiletracker.config.AppConfig
import kaist.iclab.mobiletracker.data.DeviceType
import kaist.iclab.mobiletracker.db.entity.common.LocationEntity
import kaist.iclab.mobiletracker.db.obx.LocationStore
import kaist.iclab.mobiletracker.db.obx.SupabaseJson
import kaist.iclab.mobiletracker.repository.ErrorClassifier
import kaist.iclab.mobiletracker.repository.Result
import kaist.iclab.mobiletracker.services.supabase.SupabaseUploadService
import kaist.iclab.mobiletracker.services.upload.handlers.SensorUploadHandler
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/**
 * Upload handler for phone Location data (deviceType = PHONE), backed by [LocationStore].
 * Serializes [LocationEntity] directly to its Supabase row (no mapper/DTO).
 */
class LocationUploadHandler(
    private val store: LocationStore,
    private val supabase: SupabaseUploadService
) : SensorUploadHandler {
    override val sensorId = "Location"
    private val deviceType = DeviceType.PHONE.value

    private fun toRow(entity: LocationEntity, userUuid: String): JsonObject {
        val obj = SupabaseJson.encodeToJsonElement(LocationEntity.serializer(), entity).jsonObject
        return JsonObject(obj + ("uuid" to JsonPrimitive(userUuid)))
    }

    override suspend fun hasDataToUpload(lastUploadTimestamp: Long): Boolean =
        store.hasDataAfterByDeviceType(lastUploadTimestamp, deviceType)

    override suspend fun uploadData(userUuid: String, lastUploadTimestamp: Long): Result<Long> {
        return ErrorClassifier.runClassified(sensorId, "upload $sensorId") {
            val batchSize = Constants.Network.UPLOAD_BATCH_SIZE
            var currentMaxTimestamp = lastUploadTimestamp
            var uploadedAny = false

            while (true) {
                val entities = store.recordsAfterByDeviceType(
                    afterTimestamp = currentMaxTimestamp + 1,
                    isAscending = true,
                    limit = batchSize,
                    offset = 0,
                    deviceType = deviceType
                )

                if (entities.isEmpty()) break

                val rows = entities.map { toRow(it, userUuid) }
                supabase.upsertBatch(AppConfig.SupabaseTables.LOCATION_SENSOR, "location", rows)
                    .getOrElse { throw it }

                currentMaxTimestamp = entities.maxOf { it.timestamp }
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
        store.removeBeforeByDeviceType(beforeTimestamp, deviceType)
    }

    override suspend fun getRecordCount(): Int = store.countByDeviceType(deviceType)

    override suspend fun getRecordsPaginated(limit: Int, offset: Int): List<Any> =
        store.recordsAfterByDeviceType(0L, true, limit, offset, deviceType)

    override fun getCsvHeader(): String =
        "eventId,uuid,deviceType,received,timestamp,latitude,longitude,altitude,speed,accuracy"

    override fun recordToCsvRow(record: Any): String {
        val entity = record as LocationEntity
        return "${entity.eventId},${entity.uuid},${entity.deviceType},${entity.received},${entity.timestamp},${entity.latitude},${entity.longitude},${entity.altitude},${entity.speed},${entity.accuracy}"
    }
}
