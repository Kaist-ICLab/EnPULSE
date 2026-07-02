package kaist.iclab.mobiletracker.services.upload.handlers.phone

import kaist.iclab.mobiletracker.db.entity.phone.BatteryEntity
import kaist.iclab.mobiletracker.db.mapper.BatteryMapper
import kaist.iclab.mobiletracker.db.obx.SensorStore
import kaist.iclab.mobiletracker.repository.ErrorClassifier
import kaist.iclab.mobiletracker.repository.Result
import kaist.iclab.mobiletracker.services.supabase.BatterySensorService
import kaist.iclab.mobiletracker.services.upload.handlers.SensorUploadHandler

/**
 * Upload handler for Battery sensor data, backed by the generic [SensorStore].
 * The Supabase mapper + service (remote path) are unchanged.
 */
class BatteryUploadHandler(
    private val store: SensorStore<BatteryEntity>,
    private val service: BatterySensorService
) : SensorUploadHandler {
    override val sensorId = "Battery"

    override suspend fun hasDataToUpload(lastUploadTimestamp: Long): Boolean {
        return store.hasDataAfter(lastUploadTimestamp)
    }

    override suspend fun uploadData(userUuid: String, lastUploadTimestamp: Long): Result<Long> {
        return ErrorClassifier.runClassified(sensorId, "upload $sensorId") {
            val batchSize = kaist.iclab.mobiletracker.Constants.Network.UPLOAD_BATCH_SIZE
            var currentMaxTimestamp = lastUploadTimestamp
            var uploadedAny = false

            while (true) {
                val entities = store.recordsAfter(
                    afterTimestamp = currentMaxTimestamp + 1,
                    isAscending = true,
                    limit = batchSize,
                    offset = 0
                )

                if (entities.isEmpty()) break

                val supabaseDataList = entities.map { BatteryMapper.map(it, userUuid) }
                service.insertBatterySensorDataBatch(supabaseDataList)
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
        store.removeBefore(beforeTimestamp)
    }

    override suspend fun getRecordCount(): Int {
        return store.count().toInt()
    }

    override suspend fun getRecordsPaginated(limit: Int, offset: Int): List<Any> {
        return store.recordsAfter(0L, true, limit, offset)
    }

    override fun getCsvHeader(): String {
        return "eventId,uuid,received,timestamp,connectedType,status,level,temperature"
    }

    override fun recordToCsvRow(record: Any): String {
        val entity = record as BatteryEntity
        return "${entity.eventId},${entity.uuid},${entity.received},${entity.timestamp},${entity.connectedType},${entity.status},${entity.level},${entity.temperature}"
    }
}
