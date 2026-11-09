package kaist.iclab.mobiletracker.services

import android.util.Log
import kaist.iclab.mobiletracker.db.entity.phone.SurveyResponseEntity
import kaist.iclab.mobiletracker.db.obx.SensorStore
import kaist.iclab.mobiletracker.di.AppCoroutineScope
import kaist.iclab.tracker.sensor.phone.SurveySensor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

/**
 * Saves every submitted phone survey response, regardless of "Start Logging" or the per-sensor
 * `campaign_table` gate. A submission (native [kaist.iclab.tracker.sensor.survey.activity.SurveyActivity]
 * or the EnPulse webapp's `setSurveyResponse` bridge call) arrives as a
 * [SurveySensor.RESULT_ACTION_NAME] broadcast. Triggers keep running after Stop Logging and a
 * notification can outlive it, so an answer given while collection is off must still be kept. It
 * must also NOT depend on `survey_sensor` happening to be in the current campaign's
 * `campaign_table` or on a per-sensor Settings toggle (unlike other sensors, survey has neither).
 *
 * That's why [SurveySensor] is deliberately **not** in the `"phoneSensors"` list
 * ([kaist.iclab.mobiletracker.di.phone.ControllerModule]): there, `BackgroundController` only
 * `start()`s a sensor whose own `SensorState` is `ENABLED` — and that toggle can only ever reach
 * `ENABLED` if `survey_sensor` is in `campaign_table` (see `SettingsViewModel`). That's the exact
 * gap that used to drop responses silently even while "Start Logging" was on. Instead,
 * [kaist.iclab.mobiletracker.MobileTrackerApplication] drives [start]/[stop] off the same
 * `BackgroundController.controllerStateFlow` transitions that start/stop
 * [PhoneSensorDataService], which only resets the survey schedule; answers are saved either way.
 *
 * Storage/upload is the regular sensor path: this class only ever writes to the "Survey" sensor
 * store (shared with watch MicroEMA answers). Upload is entirely [AutoSyncService]'s job, on the
 * same "Start Logging"/interval/network-gated cycle as every other sensor in
 * [kaist.iclab.mobiletracker.services.upload.DataUploadService].
 */
class SurveyResponseCapture(
    private val surveySensor: SurveySensor,
    private val surveyResponseStore: SensorStore<SurveyResponseEntity>,
    private val appScope: AppCoroutineScope
) {
    private var isStarted = false

    init {
        surveySensor.addListener { entity -> saveResponse(entity) }
    }

    private fun saveResponse(entity: SurveySensor.Entity) {
        appScope.io.launch {
            handleSurveyResponse(entity)
        }
    }

    /**
     * Call whenever data collection starts (mirrors [PhoneSensorDataService.start]). Idempotent —
     * safe to call on every `RUNNING` emission from `controllerStateFlow`, including
     * RUNNING→PAUSED→RUNNING resumes.
     */
    fun start() {
        if (isStarted) return
        isStarted = true
        surveySensor.start()
    }

    /** Call whenever data collection stops (mirrors [PhoneSensorDataService.stop]). Idempotent. */
    fun stop() {
        if (!isStarted) return
        isStarted = false
        surveySensor.stop()
    }

    private suspend fun handleSurveyResponse(surveyEntity: SurveySensor.Entity) {
        try {
            val entities = buildSurveyResponseEntities(surveyEntity)
            if (entities.isEmpty()) {
                // Was there anything to parse at all? If so, this is a shape mismatch worth
                // surfacing loudly — it used to fail silently (nothing stored, nothing logged).
                Log.w(TAG, "Survey response had no parseable questions")
                return
            }

            surveyResponseStore.insertBatch(entities)
            // No immediate flush — AutoSyncService's gated cycle uploads this, same as every
            // other sensor.
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Error processing response", e)
        }
    }

    /**
     * Accepts two shapes: a [JsonArray] of per-question objects — what the native
     * [kaist.iclab.tracker.sensor.survey.Survey.getSurveyResponse] sends, one call submitting
     * every question at once — or a single [JsonObject], for callers (e.g. a webapp bridge
     * submitting one question's answer per call) that don't batch. Either way each object needs
     * an `"id"` field identifying the question.
     */
    private fun buildSurveyResponseEntities(entity: SurveySensor.Entity): List<SurveyResponseEntity> {
        val responseJson = entity.response
        val elements = when (responseJson) {
            is JsonArray -> responseJson
            is JsonObject -> listOf(responseJson)
            else -> return emptyList()
        }

        // Null moments (e.g. no scheduleId) fall back to "now".
        val now = System.currentTimeMillis()
        val triggerTime = entity.triggerTime ?: now
        val actualTriggerTime = entity.actualTriggerTime ?: now
        val surveyStartTime = entity.surveyStartTime ?: now
        val responseSubmissionTime = entity.responseSubmissionTime?.takeIf { it > 0 } ?: now

        return elements.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val questionId =
                obj["id"]?.toString()?.replace("\"", "")?.toIntOrNull() ?: return@mapNotNull null

            SurveyResponseEntity(
                questionId = questionId,
                triggerTime = triggerTime,
                actualTriggerTime = actualTriggerTime,
                surveyStartTime = surveyStartTime,
                responseSubmissionTime = responseSubmissionTime,
                responseJson = element.toString()
            )
        }
    }

    companion object {
        private const val TAG = "SurveyResponseCapture"
    }
}
