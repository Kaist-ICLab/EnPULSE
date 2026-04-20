package kaist.iclab.mobiletracker.services.upload.handlers.phone

import kaist.iclab.mobiletracker.db.dao.phone.MediaDao
import kaist.iclab.mobiletracker.db.mapper.MediaMapper
import kaist.iclab.mobiletracker.repository.ErrorClassifier
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

                val supabaseDataList = entities.map { MediaMapper.map(it, userUuid) }
                service.insertMediaSensorDataBatch(supabaseDataList)
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
        return "eventId,uuid,received,timestamp,operation,mediaType,storageType,uri,fileName,mimeType,size,dateAdded,dateModified"
    }

    override fun recordToCsvRow(record: Any): String {
        val entity = record as kaist.iclab.mobiletracker.db.entity.phone.MediaEntity
        return "${entity.eventId},${entity.uuid},${entity.received},${entity.timestamp},${entity.operation},${entity.mediaType},${entity.storageType},${entity.uri},${entity.fileName},${entity.mimeType},${entity.size},${entity.dateAdded},${entity.dateModified}"
    }
}
