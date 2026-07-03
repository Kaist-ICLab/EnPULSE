package kaist.iclab.mobiletracker.services.upload.handlers

import kaist.iclab.mobiletracker.Constants
import kaist.iclab.mobiletracker.db.obx.SensorStore
import kaist.iclab.mobiletracker.repository.ErrorClassifier
import kaist.iclab.mobiletracker.repository.Result

/**
 * Generic upload handler backed by a [SensorStore]. The paginate → map → upload → track-max loop
 * and the prune/count/pagination methods were identical across every sensor; only the Supabase
 * mapping ([toSupabase]), the batch upload call ([uploadBatch]) and the CSV shape differ.
 *
 * @param T stored entity type, @param S Supabase row type.
 */
class GenericSensorUploadHandler<T : Any, S : Any>(
    override val sensorId: String,
    private val store: SensorStore<T>,
    private val timestampOf: (T) -> Long,
    private val toSupabase: (T, String?) -> S,
    private val uploadBatch: suspend (List<S>) -> Result<Unit>,
    private val csvHeader: String,
    private val toCsvRow: (T) -> String
) : SensorUploadHandler {

    override suspend fun hasDataToUpload(lastUploadTimestamp: Long): Boolean =
        store.hasDataAfter(lastUploadTimestamp)

    override suspend fun uploadData(userUuid: String, lastUploadTimestamp: Long): Result<Long> {
        return ErrorClassifier.runClassified(sensorId, "upload $sensorId") {
            val batchSize = Constants.Network.UPLOAD_BATCH_SIZE
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

                uploadBatch(entities.map { toSupabase(it, userUuid) }).getOrElse { throw it }

                currentMaxTimestamp = entities.maxOf { timestampOf(it) }
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

    override suspend fun getRecordCount(): Int = store.count().toInt()

    override suspend fun getRecordsPaginated(limit: Int, offset: Int): List<Any> =
        store.recordsAfter(0L, true, limit, offset)

    override fun getCsvHeader(): String = csvHeader

    @Suppress("UNCHECKED_CAST")
    override fun recordToCsvRow(record: Any): String = toCsvRow(record as T)
}
