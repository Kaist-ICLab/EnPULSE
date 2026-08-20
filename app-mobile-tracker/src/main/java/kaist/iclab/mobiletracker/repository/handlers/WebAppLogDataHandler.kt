package kaist.iclab.mobiletracker.repository.handlers

import kaist.iclab.mobiletracker.config.AppConfig
import kaist.iclab.mobiletracker.db.obx.WebAppLogStore
import kaist.iclab.mobiletracker.repository.SensorRecord
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * [SensorDataHandler] for locally queued webapp analytics events ([WebAppLogStore]), so they show
 * up on the Data screen with a record count and get a manual "Upload Now" button like every other
 * sensor. Kept deliberately light on per-record detail (webapp id / event type / synced flag
 * only, no `properties` payload) — webapp logs are debug/analytics telemetry, not primary study
 * data, so a count is what the screen is really for.
 *
 * Hand-written for the same reason as [SurveyResponseDataHandler]: not a
 * [kaist.iclab.mobiletracker.db.entity.BaseEntity], insert-only, uploaded by
 * [kaist.iclab.mobiletracker.services.upload.WebAppLogUploader] rather than the generic
 * upsert-by-eventId [kaist.iclab.mobiletracker.services.upload.handlers.SensorUploadHandlerImpl].
 * [kaist.iclab.mobiletracker.repository.DataRepositoryImpl] special-cases [SENSOR_ID] the same way
 * it does [SurveyResponseDataHandler.SENSOR_ID]: never campaign-gated, and manual upload routes to
 * [kaist.iclab.mobiletracker.services.upload.WebAppLogUploader] instead of
 * [kaist.iclab.mobiletracker.services.upload.SensorUploadService].
 */
class WebAppLogDataHandler(
    private val store: WebAppLogStore
) : SensorDataHandler {
    override val sensorId: String = SENSOR_ID
    override val displayName: String = "Web App Log"
    override val isWatchSensor: Boolean = false
    override val supabaseTableName: String = AppConfig.SupabaseTables.WEB_APP_LOG

    /** `web_app_log` has no per-row remote id column; unused since [getEventIdById] is always null. */
    override val remoteIdColumnName: String = "uuid"

    override suspend fun getRecordCount(): Int = store.count().toInt()

    override suspend fun getLatestTimestamp(): Long? = store.latestTimestamp()

    override suspend fun getRecordCountAfterTimestamp(timestamp: Long): Int =
        store.countAfter(timestamp).toInt()

    /** Records Supabase has actually accepted — see [WebAppLogStore.syncedCount]. */
    suspend fun getUploadedRecordCount(): Long = store.syncedCount()

    override suspend fun getRecordsPaginated(
        afterTimestamp: Long,
        isAscending: Boolean,
        limit: Int,
        offset: Int
    ): List<SensorRecord> =
        store.recordsAfter(afterTimestamp, isAscending, limit, offset).map { entity ->
            SensorRecord(
                id = entity.id,
                timestamp = entity.timestamp,
                fields = mapOf(
                    "webapp_id" to entity.webAppId,
                    "event_type" to entity.eventType,
                    "synced" to entity.isSynced.toString()
                )
            )
        }

    override suspend fun getRecordsJsonPaginated(
        afterTimestamp: Long,
        beforeTimestamp: Long,
        isAscending: Boolean,
        limit: Int,
        offset: Int
    ): List<JsonElement> =
        store.recordsAfter(afterTimestamp, isAscending, limit, offset)
            .filter { it.timestamp <= beforeTimestamp }
            .map { entity ->
                buildJsonObject {
                    put("webapp_id", entity.webAppId)
                    put("event_type", entity.eventType)
                    put("timestamp", entity.timestamp)
                    put("synced", entity.isSynced)
                }
            }

    override suspend fun deleteAll() = store.removeAll()

    override suspend fun deleteById(id: Long) {
        store.removeById(id)
    }

    /** No per-row remote id is tracked locally (insert-only, dedup isn't needed). */
    override suspend fun getEventIdById(id: Long): String? = null

    companion object {
        const val SENSOR_ID = "WebAppLog"
    }
}
