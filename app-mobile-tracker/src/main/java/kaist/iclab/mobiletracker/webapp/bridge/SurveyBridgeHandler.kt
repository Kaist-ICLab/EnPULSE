package kaist.iclab.mobiletracker.webapp.bridge

import android.content.Context
import android.content.Intent
import kaist.iclab.mobiletracker.storage.CouchbaseSurveyConfigStorage
import kaist.iclab.mobiletracker.repository.UserProfileRepository
import kaist.iclab.tracker.sensor.phone.SurveySensor
import kaist.iclab.tracker.sensor.survey.config.SurveyConfig
import kaist.iclab.tracker.storage.core.SurveyScheduleStorage
import kotlinx.serialization.encodeToString
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Handles the `getSurvey` / `setSurveyResponse` bridge actions.
 *
 * Reuses the same [SurveyConfig] the phone already persists from Supabase (design principle #3:
 * no new storage/serialization pipeline needed) and the same [SurveyScheduleStorage] instance
 * injected into [kaist.iclab.tracker.sensor.phone.SurveySensor], so `schedule_id` lookups agree.
 */
class SurveyBridgeHandler(
    private val context: Context,
    private val surveyConfigStorage: CouchbaseSurveyConfigStorage,
    private val scheduleStorage: SurveyScheduleStorage,
    private val userProfileRepository: UserProfileRepository
) {
    fun getSurvey(request: BridgeRequest): BridgeResponse {
        val params = request.payload.jsonObject
        val surveyTitle = params["survey_title"]?.jsonPrimitive?.content
        val surveyId = params["survey_id"]?.jsonPrimitive?.content
        val scheduleId = params["schedule_id"]?.jsonPrimitive?.content

        var resolvedSurveyId = surveyId
        if (resolvedSurveyId.isNullOrBlank() && !scheduleId.isNullOrBlank()) {
            val schedule = scheduleStorage.getScheduleByScheduleId(scheduleId)
            if (schedule != null && !schedule.surveyId.isNullOrBlank()) {
                resolvedSurveyId = schedule.surveyId
            }
        }

        if (surveyTitle.isNullOrBlank() && resolvedSurveyId.isNullOrBlank()) {
            return BridgeResponse(request.requestId, "error", errorMessage = "Could not resolve survey identifier (must provide valid survey_title, survey_id, or schedule_id)")
        }

        // deviceType == 0: phone surveys only, matching SurveySensorModule's config priming.
        val survey = surveyConfigStorage.get().configs.firstOrNull { config ->
            config.deviceType == 0 &&
            (surveyTitle.isNullOrBlank() || config.title == surveyTitle) &&
            (resolvedSurveyId.isNullOrBlank() || config.id.toString() == resolvedSurveyId)
        } ?: return BridgeResponse(request.requestId, "error", errorMessage = "Unknown survey (title=$surveyTitle, id=$resolvedSurveyId)")

        if (!scheduleId.isNullOrBlank()) {
            scheduleStorage.setSurveyStartTime(scheduleId, System.currentTimeMillis())
        }

        return BridgeResponse(
            request.requestId, "success",
            data = Json.encodeToJsonElement(SurveyConfig.serializer(), survey)
        )
    }

    fun getAllSurvey(request: BridgeRequest): BridgeResponse {
        val userCampaignId = userProfileRepository.profileFlow.value?.campaignId
        val allSurveys = surveyConfigStorage.get().configs.filter {
            it.deviceType == 0 && (userCampaignId == null || it.campaignId == userCampaignId)
        }
        return BridgeResponse(
            request.requestId, "success",
            data = Json.encodeToJsonElement(ListSerializer(SurveyConfig.serializer()), allSurveys)
        )
    }

    fun setSurveyResponse(request: BridgeRequest): BridgeResponse {
        val params = request.payload.jsonObject
        val scheduleId = params["schedule_id"]?.jsonPrimitive?.content
            ?: return BridgeResponse(request.requestId, "error", errorMessage = "Missing schedule_id")
        val answers = params["answers"]
            ?: return BridgeResponse(request.requestId, "error", errorMessage = "Missing answers")

        // Same broadcast SurveyActivity.pushSurveyResult() sends — the existing
        // SurveySensor.surveyResultCallback absorbs it with no new storage logic.
        val intent = Intent(SurveySensor.RESULT_ACTION_NAME).apply {
            putExtra("result", Json.encodeToString(answers))
            putExtra("responseTime", System.currentTimeMillis())
            putExtra("scheduleId", scheduleId)
        }
        context.sendBroadcast(intent)

        return BridgeResponse(request.requestId, "success")
    }
}
