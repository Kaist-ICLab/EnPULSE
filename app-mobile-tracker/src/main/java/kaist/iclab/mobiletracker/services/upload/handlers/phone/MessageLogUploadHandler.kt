package kaist.iclab.mobiletracker.services.upload.handlers.phone

import kaist.iclab.mobiletracker.db.entity.phone.MessageLogEntity
import kaist.iclab.mobiletracker.db.mapper.MessageLogMapper
import kaist.iclab.mobiletracker.db.obx.SensorStore
import kaist.iclab.mobiletracker.repository.ErrorClassifier
import kaist.iclab.mobiletracker.repository.Result
import kaist.iclab.mobiletracker.services.supabase.MessageLogSensorService
import kaist.iclab.mobiletracker.services.upload.handlers.SensorUploadHandler

/**
 * Upload handler for Message Log sensor data.
 */
class MessageLogUploadHandler(
    private val store: SensorStore<MessageLogEntity>,
    private val service: MessageLogSensorService
) : SensorUploadHandler {
    override val sensorId = "MessageLog"

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

                val supabaseDataList = entities.map { MessageLogMapper.map(it, userUuid) }
                service.insertMessageLogSensorDataBatch(supabaseDataList)
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
        return "eventId,uuid,received,timestamp,number,messageType,contactType"
    }

    override fun recordToCsvRow(record: Any): String {
        val entity = record as kaist.iclab.mobiletracker.db.entity.phone.MessageLogEntity
        return "${entity.eventId},${entity.uuid},${entity.received},${entity.timestamp},${entity.number},${entity.messageType},${entity.contactType}"
    }
}
