package kaist.iclab.mobiletracker.services.upload.handlers

import kaist.iclab.mobiletracker.Constants
import kaist.iclab.mobiletracker.db.obx.SensorStore
import kaist.iclab.mobiletracker.db.obx.SupabaseJson
import kaist.iclab.mobiletracker.repository.ErrorClassifier
import kaist.iclab.mobiletracker.repository.Result
import kaist.iclab.mobiletracker.services.supabase.SupabaseUploadService
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/**
 * Generic upload handler backed by a [SensorStore]. The stored entity now serializes directly into
 * its Supabase row (via [serializer]), so there is no per-sensor DTO or mapper — only the table
 * name and the CSV shape differ. `uuid` is stamped with the logged-in user's UUID at upload time,
 * exactly as the old mappers did.
 *
 * @param T stored entity type (a `@Serializable` ObjectBox entity).
 */
class GenericSensorUploadHandler<T : Any>(
    override val sensorId: String,
    private val store: SensorStore<T>,
    private val timestampOf: (T) -> Long,
    private val serializer: KSerializer<T>,
    private val tableName: String,
    private val sensorName: String,
    private val supabase: SupabaseUploadService,
    private val csvHeader: String,
    private val toCsvRow: (T) -> String
) : SensorUploadHandler {

    private fun toSupabaseRow(entity: T, userUuid: String): JsonObject {
        val obj = SupabaseJson.encodeToJsonElement(serializer, entity).jsonObject
        return JsonObject(obj + ("uuid" to JsonPrimitive(userUuid)))
    }

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

                val rows = entities.map { toSupabaseRow(it, userUuid) }
                supabase.upsertBatch(tableName, sensorName, rows).getOrElse { throw it }

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
