package kaist.iclab.mobiletracker.services.upload.handlers

import android.util.Log
import kaist.iclab.mobiletracker.Constants
import kaist.iclab.mobiletracker.db.entity.BaseEntity
import kaist.iclab.mobiletracker.db.entity.CsvSerializable
import kaist.iclab.mobiletracker.db.obx.SensorStore
import kaist.iclab.mobiletracker.db.obx.SupabaseJson
import kaist.iclab.mobiletracker.repository.AppError
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

    override val displayName: String get() = sensorName

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
        allowQuarantine: Boolean,
        onBatchUploaded: suspend (cursor: Long) -> Unit,
        onBatchQuarantined: suspend (cursor: Long, recordCount: Int, reason: String) -> Unit
    ): Result<Long> {
        return ErrorClassifier.runClassified(sensorId, "upload $sensorId") {
            val batchSize = Constants.Network.UPLOAD_BATCH_SIZE
            var currentCursor = lastUploadCursor
            var uploadedRows = 0

            while (true) {
                val entities = store.recordsWithIdAfter(afterId = currentCursor, limit = batchSize)

                if (entities.isEmpty()) break

                // Everything accepted so far stays accepted: each batch's cursor was handed to
                // onBatchUploaded as it landed, so the next run resumes from here. Before that, the
                // cursor only moved once the whole sensor finished, so a failure part-way through
                // re-uploaded the entire backlog next time — and a backlog that failed at the same
                // batch every run (a row the server rejects, a batch big enough to time out) never
                // made any forward progress at all.
                val rows = entities.map { toSupabaseRow(it, userUuid) }
                when (val result = supabase.upsertBatch(tableName, sensorName, rows)) {
                    is Result.Success -> {
                        currentCursor = entities.maxOf { it.id }
                        uploadedRows += entities.size
                        onBatchUploaded(currentCursor)
                    }
                    is Result.Error -> {
                        if (allowQuarantine && result.exception is AppError.ServerRejected) {
                            // The caller has already seen this exact spot fail this way repeatedly
                            // across separate upload cycles (see SensorUploadService's streak
                            // tracking) — give up on this whole batch rather than the entire sensor,
                            // so everything captured after it can still get through. The batch stays
                            // in local storage; it's simply skipped instead of retried forever.
                            currentCursor = entities.maxOf { it.id }
                            val reason = result.exception.message ?: "Server rejected this batch"
                            Log.e(
                                TAG,
                                "Quarantining $sensorId batch of ${entities.size} records up to " +
                                    "cursor $currentCursor: $reason"
                            )
                            onBatchQuarantined(currentCursor, entities.size, reason)
                            uploadedRows += entities.size
                        } else {
                            Log.w(
                                TAG,
                                "Partial upload for $sensorId: $uploadedRows rows committed up to " +
                                    "cursor $currentCursor, resuming from there next run",
                                result.exception
                            )
                            throw result.exception
                        }
                    }
                }

                if (entities.size < batchSize) break
            }

            if (uploadedRows == 0) {
                throw IllegalStateException("No new $sensorId data to upload")
            }

            currentCursor
        }
    }

    override suspend fun pendingBatchCount(lastUploadCursor: Long): Int {
        val batchSize = Constants.Network.UPLOAD_BATCH_SIZE
        val pendingRows = store.countWithIdAfter(lastUploadCursor)
        return ((pendingRows + batchSize - 1) / batchSize).toInt()
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

    private companion object {
        const val TAG = "SensorUploadHandler"
    }
}
