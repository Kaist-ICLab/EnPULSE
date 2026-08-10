package kaist.iclab.mobiletracker.repository.handlers

import kaist.iclab.mobiletracker.Constants
import kaist.iclab.mobiletracker.db.obx.SurveyResponseStore
import kaist.iclab.mobiletracker.repository.SensorRecord
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * [SensorDataHandler] for locally queued phone survey responses ([SurveyResponseStore]), so they
 * show up on the Data screen and get a manual "Upload Now" button like every other sensor.
 *
 * Hand-written rather than a [kaist.iclab.mobiletracker.di.SensorDescriptor]-driven
 * [GenericSensorDataHandler]: survey responses aren't a [kaist.iclab.mobiletracker.db.entity.BaseEntity]
 * (no `event_id`/`device_type`/`received` columns) and upload insert-only via
 * [kaist.iclab.mobiletracker.services.upload.SurveyResponseUploader], not the upsert-by-eventId
 * shape [kaist.iclab.mobiletracker.services.upload.handlers.SensorUploadHandlerImpl] assumes.
 *
 * [kaist.iclab.mobiletracker.repository.DataRepositoryImpl] special-cases [SENSOR_ID] so it's
 * never hidden by the campaign active-sensor gate and its manual upload goes through
 * [kaist.iclab.mobiletracker.services.upload.SurveyResponseUploader] instead of the generic
 * [kaist.iclab.mobiletracker.services.upload.SensorUploadService] — matching how survey response
 * *capture* already ignores `campaign_table` (see `PhoneSensorDataService.registerListeners`).
 */
class SurveyResponseDataHandler(
    private val store: SurveyResponseStore
) : SensorDataHandler {
    override val sensorId: String = SENSOR_ID
    override val displayName: String = "Survey"
    override val isWatchSensor: Boolean = false
    override val supabaseTableName: String = Constants.DB.TABLE_RESPONSE

    /** `survey_question_response` has no per-row remote id column to delete by. */
    override val remoteIdColumnName: String = "question_id"

    override suspend fun getRecordCount(): Int = store.count().toInt()

    override suspend fun getLatestTimestamp(): Long? = store.latestTimestamp()

    override suspend fun getRecordCountAfterTimestamp(timestamp: Long): Int =
        store.countAfter(timestamp).toInt()

    override suspend fun getRecordsPaginated(
        afterTimestamp: Long,
        isAscending: Boolean,
        limit: Int,
        offset: Int
    ): List<SensorRecord> =
        store.recordsAfter(afterTimestamp, isAscending, limit, offset).map { entity ->
            SensorRecord(
                id = entity.id,
                timestamp = entity.responseSubmissionTime,
                fields = mapOf(
                    "question_id" to entity.questionId.toString(),
                    "response" to entity.responseJson,
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
            .filter { it.responseSubmissionTime <= beforeTimestamp }
            .map { entity ->
                buildJsonObject {
                    put("question_id", entity.questionId)
                    put("trigger_time", entity.triggerTime)
                    put("actual_trigger_time", entity.actualTriggerTime)
                    put("survey_start_time", entity.surveyStartTime)
                    put("response_submission_time", entity.responseSubmissionTime)
                    put("response", Json.parseToJsonElement(entity.responseJson))
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
        const val SENSOR_ID = "Survey"
    }
}
