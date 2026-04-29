package kaist.iclab.mobiletracker.services.upload.handlers.watch

import kaist.iclab.mobiletracker.Constants
import kaist.iclab.mobiletracker.db.dao.watch.WatchGestureDao
import kaist.iclab.mobiletracker.db.mapper.GestureMapper
import kaist.iclab.mobiletracker.repository.ErrorClassifier
import kaist.iclab.mobiletracker.repository.Result
import kaist.iclab.mobiletracker.services.supabase.GestureSensorService
import kaist.iclab.mobiletracker.services.upload.handlers.SensorUploadHandler

class WatchGestureUploadHandler(
    private val dao: WatchGestureDao,
    private val service: GestureSensorService
) : SensorUploadHandler {
    override val sensorId = "WatchGesture"

    override suspend fun hasDataToUpload(lastUploadTimestamp: Long): Boolean {
        return dao.hasDataAfterTimestamp(lastUploadTimestamp)
    }

    override suspend fun uploadData(userUuid: String, lastUploadTimestamp: Long): Result<Long> {
        return ErrorClassifier.runClassified(sensorId, "upload $sensorId") {
            val batchSize = Constants.Network.UPLOAD_BATCH_SIZE
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

                val supabaseDataList = entities.map { GestureMapper.map(it, userUuid) }
                service.insertGestureSensorDataBatch(supabaseDataList)
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
        return "eventId,uuid,deviceType,received,timestamp,classIndex,score,probabilities"
    }

    override fun recordToCsvRow(record: Any): String {
        val entity = record as kaist.iclab.mobiletracker.db.entity.watch.WatchGestureEntity
        val escapedProbs = entity.probabilities.joinToString(";").replace("\"", "\"\"")
        return "${entity.eventId},${entity.uuid},${entity.deviceType},${entity.received},${entity.timestamp},${entity.classIndex},${entity.score},\"[$escapedProbs]\""
    }
}
