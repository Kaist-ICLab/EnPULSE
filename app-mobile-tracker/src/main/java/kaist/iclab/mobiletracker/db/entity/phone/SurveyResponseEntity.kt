package kaist.iclab.mobiletracker.db.entity.phone

import io.objectbox.annotation.Entity
import kaist.iclab.mobiletracker.data.DeviceType
import kaist.iclab.mobiletracker.db.entity.BaseEntity
import kaist.iclab.mobiletracker.db.entity.CsvSerializable
import kaist.iclab.mobiletracker.db.entity.RecordSerializable
import kaist.iclab.mobiletracker.db.obx.EpochMillisIsoSerializer
import kaist.iclab.mobiletracker.db.obx.JsonStringElementSerializer
import kaist.iclab.mobiletracker.repository.SensorRecord
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * One survey question response, from either device: phone surveys
 * ([kaist.iclab.mobiletracker.services.SurveyResponseCapture], [deviceType] = PHONE) and watch
 * MicroEMA answers ([kaist.iclab.mobiletracker.helpers.BLEHelper], [deviceType] = WATCH). One row
 * per question, matching the `survey_question_response` table's grain.
 *
 * Stored and uploaded like any other sensor (the "Survey" entry in
 * [kaist.iclab.mobiletracker.di.SensorRegistry]), with two differences declared on that entry:
 * it is not campaign-gated, and the table has none of [BaseEntity]'s columns, so rows are
 * inserted with only [SUPABASE_COLUMNS] instead of upserted by `event_id`.
 *
 * [timestamp] mirrors [responseSubmissionTime]. `uuid` is stamped at upload time, so a response
 * captured before login/session-restore still queues.
 */
@Entity
@Serializable
class SurveyResponseEntity : BaseEntity, CsvSerializable, RecordSerializable {
    @SerialName("question_id")
    var questionId: Int = 0

    @SerialName("trigger_time")
    @Serializable(with = EpochMillisIsoSerializer::class)
    var triggerTime: Long = 0

    @SerialName("actual_trigger_time")
    @Serializable(with = EpochMillisIsoSerializer::class)
    var actualTriggerTime: Long = 0

    @SerialName("survey_start_time")
    @Serializable(with = EpochMillisIsoSerializer::class)
    var surveyStartTime: Long = 0

    @SerialName("response_submission_time")
    @Serializable(with = EpochMillisIsoSerializer::class)
    var responseSubmissionTime: Long = 0

    /** JSON text of this question's `response` element (`{"id": ..., "value": ...}`). */
    @SerialName("response")
    @Serializable(with = JsonStringElementSerializer::class)
    var responseJson: String = ""

    constructor()

    constructor(
        id: Long = 0,
        eventId: String = UUID.randomUUID().toString(),
        uuid: String = "",
        received: Long = System.currentTimeMillis(),
        deviceType: Int = DeviceType.PHONE.value,
        questionId: Int = 0,
        triggerTime: Long = 0,
        actualTriggerTime: Long = 0,
        surveyStartTime: Long = 0,
        responseSubmissionTime: Long = 0,
        responseJson: String = "",
    ) {
        initBaseEntity(id, eventId, uuid, received, responseSubmissionTime, deviceType)

        this.questionId = questionId
        this.triggerTime = triggerTime
        this.actualTriggerTime = actualTriggerTime
        this.surveyStartTime = surveyStartTime
        this.responseSubmissionTime = responseSubmissionTime
        this.responseJson = responseJson
    }

    override fun csvHeader(): String = "eventId,uuid,received,deviceType,questionId,triggerTime,actualTriggerTime,surveyStartTime,responseSubmissionTime,responseJson"

    override fun toCsvRow(): String = "$eventId,$uuid,$received,$deviceType,$questionId,$triggerTime,$actualTriggerTime,$surveyStartTime,$responseSubmissionTime,$responseJson"

    override fun toRecord() = SensorRecord(
        id = id,
        timestamp = timestamp,
        fields = mapOf(
            "question_id" to questionId.toString(),
            "response" to responseJson
        )
    )

    companion object {
        const val SENSOR_ID = "Survey"

        /** The `survey_question_response` columns; everything else in the entity is local-only. */
        val SUPABASE_COLUMNS = setOf(
            "uuid",
            "question_id",
            "trigger_time",
            "actual_trigger_time",
            "survey_start_time",
            "response_submission_time",
            "response"
        )
    }
}
