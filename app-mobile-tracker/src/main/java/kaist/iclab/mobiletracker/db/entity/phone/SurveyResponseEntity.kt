package kaist.iclab.mobiletracker.db.entity.phone

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index

/**
 * ObjectBox-backed local cache for one phone survey question response
 * ([kaist.iclab.tracker.sensor.phone.SurveySensor.Entity] carries every question's answer for a
 * submission; one row here per question, matching the `survey_question_response` table's grain).
 *
 * Exists so a submission survives even when the immediate Supabase insert can't happen (offline,
 * session not restored yet, transient failure) — before this,
 * [kaist.iclab.mobiletracker.services.PhoneSensorDataService] inserted straight into Supabase with
 * no local durability, so any such failure silently dropped the response.
 *
 * Deliberately **not** a sensor: it does not extend
 * [kaist.iclab.mobiletracker.db.entity.BaseEntity], is absent from
 * [kaist.iclab.mobiletracker.db.obx.SensorStores]/[kaist.iclab.mobiletracker.di.SensorRegistry],
 * and never rides the campaign-gated sensor upload path — mirrors [WebAppLogEntity].
 *
 * `uuid` is deliberately NOT stored here; it's stamped at upload time from the current session by
 * [kaist.iclab.mobiletracker.services.upload.SurveyResponseUploader] (same as [WebAppLogEntity]),
 * so a response captured before login/session-restore still queues instead of being dropped.
 */
@Entity
data class SurveyResponseEntity(
    @Id var id: Long = 0,

    var questionId: Int = 0,

    /** Epoch millis; null moments on the source [SurveySensor.Entity] are stamped with "now". */
    var triggerTime: Long = 0,
    var actualTriggerTime: Long = 0,
    var surveyStartTime: Long = 0,
    var responseSubmissionTime: Long = 0,

    /** JSON text of this question's `response` element (`{"id": ..., "value": ...}`). */
    var responseJson: String = "",

    @Index var isSynced: Boolean = false
)
