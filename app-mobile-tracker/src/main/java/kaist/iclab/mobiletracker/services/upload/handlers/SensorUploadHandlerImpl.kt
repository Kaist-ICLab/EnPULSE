package kaist.iclab.mobiletracker.services.upload.handlers

import android.util.Log
import kaist.iclab.mobiletracker.Constants
import kaist.iclab.mobiletracker.db.entity.BaseEntity
import kaist.iclab.mobiletracker.db.entity.CsvSerializable
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
 * @param T stored entity type (a `@Serializable` ObjectBox entity extending [BaseEntity]).
 */
class SensorUploadHandlerImpl<T>(
    override val sensorId: String,
    private val store: SensorStore<T>,
    private val serializer: KSerializer<T>,
    private val tableName: String,
    private val sensorName: String,
    private val supabase: SupabaseUploadService
) : SensorUploadHandler where T : BaseEntity, T : CsvSerializable {

    private fun toSupabaseRow(entity: T, userUuid: String): JsonObject {
        val obj = SupabaseJson.encodeToJsonElement(serializer, entity).jsonObject
        return JsonObject(obj + ("uuid" to JsonPrimitive(userUuid)))
    }

    // Cursor is the local ObjectBox row id (not the record's own `timestamp`) — see
    // SensorStore.hasDataWithIdAfter's doc comment for why: id is assigned in true insertion
    // order, so it stays monotonic even when a watch BLE reconnect/backfill delivers events
    // whose own timestamp is older than data already uploaded.
    override suspend fun hasDataToUpload(lastUploadCursor: Long): Boolean =
        store.hasDataWithIdAfter(lastUploadCursor)

    override suspend fun uploadData(
        userUuid: String,
        lastUploadCursor: Long,
        onBatchUploaded: suspend (Long) -> Unit
    ): Result<Long> {
        return ErrorClassifier.runClassified(sensorId, "upload $sensorId") {
            val batchSize = Constants.Network.UPLOAD_BATCH_SIZE
            var currentCursor = lastUploadCursor
            var uploadedAny = false
            var batchIndex = 0

            while (true) {
                val entities = store.recordsWithIdAfter(afterId = currentCursor, limit = batchSize)

                if (entities.isEmpty()) break

                batchIndex++
                // recordsWithIdAfter orders by id ascending, so first/last id is the range as-is;
                // timestamp isn't guaranteed sorted, hence minOf/maxOf. If upsertBatch below throws,
                // this line (already in logcat) plus the classified error that follows pinpoints
                // exactly which local rows (id/timestamp/eventId range) caused it — debug aid for
                // tracking down which imported/captured batch triggers an upload failure.
                Log.d(
                    sensorId,
                    "Uploading $sensorId batch #$batchIndex: ${entities.size} records, " +
                        "id range [${entities.first().id}, ${entities.last().id}], " +
                        "timestamp range [${entities.minOf { it.timestamp }}, ${entities.maxOf { it.timestamp }}], " +
                        "eventId range [${entities.first().eventId}, ${entities.last().eventId}]"
                )

                val rows = entities.map { toSupabaseRow(it, userUuid) }
                supabase.upsertBatch(tableName, sensorName, rows).getOrElse { throw it }

                currentCursor = entities.maxOf { it.id }
                uploadedAny = true

                // Persist progress right away — if a later batch throws, everything up to and
                // including this one is already committed to Supabase, so it must not be silently
                // discarded and re-uploaded on the next attempt just because a later batch failed.
                onBatchUploaded(currentCursor)

                Log.d(sensorId, "Uploaded $sensorId batch #$batchIndex successfully (${entities.size} records), cursor now $currentCursor")

                if (entities.size < batchSize) break
            }

            if (!uploadedAny) {
                throw IllegalStateException("No new $sensorId data to upload")
            }

            currentCursor
        }
    }

    override suspend fun pruneData(beforeTimestamp: Long) {
        store.removeBefore(beforeTimestamp)
    }

    override suspend fun getRecordCount(): Int = store.count()

    override suspend fun getRecordsPaginated(limit: Int, offset: Int): List<Any> =
        store.recordsAfter(0L, true, limit, offset)

    override fun getCsvHeader(): String = store.newInstance().csvHeader()

    @Suppress("UNCHECKED_CAST")
    override fun recordToCsvRow(record: Any): String = (record as T).toCsvRow()
}
