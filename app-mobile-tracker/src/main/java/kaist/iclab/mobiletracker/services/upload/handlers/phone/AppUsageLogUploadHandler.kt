package kaist.iclab.mobiletracker.services.upload.handlers.phone

import kaist.iclab.mobiletracker.db.dao.phone.AppUsageLogDao
import kaist.iclab.mobiletracker.db.mapper.AppUsageLogMapper
import kaist.iclab.mobiletracker.repository.ErrorClassifier
import kaist.iclab.mobiletracker.repository.Result
import kaist.iclab.mobiletracker.services.supabase.AppUsageLogSensorService
import kaist.iclab.mobiletracker.services.upload.handlers.SensorUploadHandler

/**
 * Upload handler for App Usage Log sensor data.
 */
class AppUsageLogUploadHandler(
    private val dao: AppUsageLogDao,
    private val service: AppUsageLogSensorService
) : SensorUploadHandler {
    override val sensorId = "AppUsage"

    override suspend fun hasDataToUpload(lastUploadTimestamp: Long): Boolean {
        return dao.hasDataAfterTimestamp(lastUploadTimestamp)
    }

    override suspend fun uploadData(userUuid: String, lastUploadTimestamp: Long): Result<Long> {
        return ErrorClassifier.runClassified(sensorId, "upload $sensorId") {
            val batchSize = kaist.iclab.mobiletracker.Constants.Network.UPLOAD_BATCH_SIZE
            var currentMaxTimestamp = lastUploadTimestamp
            var uploadedAny = false

            while (true) {
                val entities = dao.getRecordsPaginated(
                    afterTimestamp = currentMaxTimestamp + 1,
                    isAscending = true,
                    limit = batchSize,
                    offset = 0
                )

                if (entities.isEmpty()) break

                val supabaseDataList = entities.map { AppUsageLogMapper.map(it, userUuid) }
                service.insertAppUsageLogSensorDataBatch(supabaseDataList)
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
        dao.deleteDataBefore(beforeTimestamp)
    }

    override suspend fun getRecordCount(): Int {
        return dao.getRecordCount()
    }

    override suspend fun getRecordsPaginated(limit: Int, offset: Int): List<Any> {
        return dao.getRecordsPaginated(0L, true, limit, offset)
    }

    override fun getCsvHeader(): String {
        return "eventId,uuid,received,timestamp,packageName,installedBy,eventType"
    }

    override fun recordToCsvRow(record: Any): String {
        val entity = record as kaist.iclab.mobiletracker.db.entity.phone.AppUsageLogEntity
        return "${entity.eventId},${entity.uuid},${entity.received},${entity.timestamp},${entity.packageName},${entity.installedBy},${entity.eventType}"
    }
}
