package kaist.iclab.mobiletracker.services


import android.util.Log
import io.github.jan.supabase.postgrest.from
import kaist.iclab.mobiletracker.Constants
import kaist.iclab.mobiletracker.data.survey.SurveyEntity
import kaist.iclab.mobiletracker.data.survey.SurveyQuestionEntity
import kaist.iclab.mobiletracker.data.survey.SurveyQuestionResponseInsert
import kaist.iclab.mobiletracker.data.survey.SurveyQuestionTriggerEntity
import kaist.iclab.mobiletracker.db.obx.MicroEmaResponseStore
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.repository.ErrorClassifier
import kaist.iclab.mobiletracker.repository.Result
import kaist.iclab.mobiletracker.repository.runCatchingSuspend
import kaist.iclab.mobiletracker.utils.DateTimeFormatter
import kaist.iclab.mobiletracker.utils.SupabaseLoadingInterceptor
import kaist.iclab.tracker.sensor.survey.config.QuestionConfig
import kaist.iclab.tracker.sensor.survey.config.ScheduleType
import kaist.iclab.tracker.sensor.survey.config.SurveyConfig
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject

/**
 * Service for fetching survey configuration from Supabase
 */
class SurveyService(
    supabaseHelper: SupabaseHelper
) {
    private val supabaseClient = supabaseHelper.supabaseClient

    companion object {
        private const val TAG = "SurveyService"
    }

    /**
     * Fetch a single survey configuration by ID from Supabase.
     * @param surveyId Unique ID of the survey.
     * @return Result containing the assembled SurveyConfig.
     */
    suspend fun getSurveyConfig(surveyId: Int): Result<SurveyConfig> {
        return SupabaseLoadingInterceptor.withLoading {
            runCatchingSuspend {
                val surveys = fetchFullSurveys {
                    eq("id", surveyId)
                }
                surveys.firstOrNull()
                    ?: throw NoSuchElementException("Survey with ID $surveyId not found")
            }
        }
    }

    /**
     * Fetch all surveys associated with a specific campaign.
     * @param campaignId Unique ID of the campaign.
     * @return Result containing a list of SurveyConfigs.
     */
    suspend fun getCampaignSurveys(campaignId: Int): Result<List<SurveyConfig>> {
        return SupabaseLoadingInterceptor.withLoading {
            ErrorClassifier.runClassified(TAG, "getCampaignSurveys($campaignId)") {
                fetchFullSurveys {
                    eq("campaign_id", campaignId)
                }
            }
        }
    }

    /**
     * Fetch all surveys specifically configured for wearable devices (watch) within a campaign.
     * Filters for surveys where device_type is 1.
     * @param campaignId Unique ID of the campaign.
     * @return Result containing a list of wearable-specific SurveyConfigs.
     */
    suspend fun getWatchSurveys(campaignId: Int): Result<List<SurveyConfig>> {
        return SupabaseLoadingInterceptor.withLoading {
            ErrorClassifier.runClassified(TAG, "getWatchSurveys($campaignId)") {
                fetchFullSurveys {
                    eq("campaign_id", campaignId)
                    eq("device_type", 1) // Watch
                }
            }
        }
    }

    /**
     * Orchestrates the fetching of surveys and all their related entities (questions, options, triggers)
     * in an optimized manner using bulk queries where possible.
     * @param filter A Postgrest filter block to apply to the initial survey selection.
     * @return A list of assembled SurveyConfig objects.
     */
    private suspend fun fetchFullSurveys(
        filter: io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder.() -> Unit
    ): List<SurveyConfig> {
        // 1. Fetch surveys based on the provided filter (ID, Campaign, Device Type, etc.)
        val surveys = supabaseClient.from(Constants.DB.TABLE_SURVEY)
            .select { filter(filter) }
            .decodeList<SurveyEntity>()

        if (surveys.isEmpty()) return emptyList()

        val surveyIds = surveys.map { it.id }

        // 2. Bulk fetch all related questions for the identified surveys
        val questions = supabaseClient.from(Constants.DB.TABLE_QUESTION)
            .select { filter { isIn("survey_id", surveyIds) } }
            .decodeList<SurveyQuestionEntity>()

        // 3. Bulk fetch all conditional triggers associated with the questions
        val triggerIds = questions.mapNotNull { it.triggeredBy }
        val triggers = if (triggerIds.isNotEmpty()) {
            supabaseClient.from(Constants.DB.TABLE_TRIGGER)
                .select { filter { isIn("id", triggerIds) } }
                .decodeList<SurveyQuestionTriggerEntity>()
        } else emptyList()

        // 4. Assemble the flat entities into a hierarchical configuration structure
        return surveys.map { survey ->
            val surveyQuestions = questions.filter { it.surveyId == survey.id }
            val surveyTriggerIds = surveyQuestions.mapNotNull { it.triggeredBy }
            val surveyTriggers = triggers.filter { it.id in surveyTriggerIds }

            assembleConfig(survey, surveyQuestions, surveyTriggers)
        }
    }

    /**
     * Maps flat database entities into the SurveyConfig domain model.
     * Handles trigger mapping and hierarchical question structure.
     */
    private fun assembleConfig(
        survey: SurveyEntity,
        questions: List<SurveyQuestionEntity>,
        triggers: List<SurveyQuestionTriggerEntity>
    ): SurveyConfig {
        val triggerMap = triggers.associateBy { it.id }
        return SurveyConfig(
            id = survey.id,
            campaignId = survey.campaignId,
            title = survey.title,
            description = survey.description,
            scheduleType = getScheduleType(survey.scheduleMethod),
            schedule = survey.scheduleMethod?.toString(),
            deviceType = survey.deviceType ?: 0,
            expireAfterMs = survey.expireAfterMs,
            questions = questions.map { q ->
                val trigger = triggerMap[q.triggeredBy]
                val config = q.config
                QuestionConfig(
                    id = q.id,
                    parentId = trigger?.questionId,
                    type = q.answerType.uppercase(),
                    text = q.question,
                    isMandatory = q.isMandatory,
                    trigger = trigger?.expression?.toString(),
                    allowFreeResponse = config.boolean("allowFreeResponse") ?: false,
                    freeResponsePrefix = config.string("freeResponsePrefix"),
                    min = config.int("min"),
                    max = config.int("max"),
                    minLabel = config.string("minLabel"),
                    maxLabel = config.string("maxLabel"),
                    options = config.stringArray("options")
                )
            }
        )
    }

    private fun JsonObject?.string(key: String): String? =
        primitive(key)?.content?.takeIf { it.isNotBlank() }

    private fun JsonObject?.int(key: String): Int? =
        primitive(key)?.intOrNull

    private fun JsonObject?.boolean(key: String): Boolean? =
        primitive(key)?.content?.toBooleanStrictOrNull()

    private fun JsonObject?.stringArray(key: String): List<String>? =
        this?.get(key)?.let { element ->
            element as? kotlinx.serialization.json.JsonArray
        }?.mapNotNull { element ->
            (element as? JsonPrimitive)?.content
        }

    private fun JsonObject?.primitive(key: String): JsonPrimitive? =
        this?.get(key) as? JsonPrimitive

    /**
     * Logic to determine the Survey Schedule Type based on the JSON keys present in the schedule_method field.
     */
    private fun getScheduleType(scheduleMethod: JsonObject?): String {
        return when {
            scheduleMethod == null -> ScheduleType.MANUAL
            scheduleMethod.containsKey("timeOfDay") -> ScheduleType.TIME_OF_DAY
            scheduleMethod.containsKey("numSurvey") -> ScheduleType.ESM
            else -> ScheduleType.MANUAL
        }
    }

    /**
     * Directly inserts a list of survey responses into the Supabase 'survey_question_response' table.
     */
    suspend fun submitSurveyResponses(responses: List<SurveyQuestionResponseInsert>): Result<Unit> {
        return ErrorClassifier.runClassified(TAG, "submitSurveyResponses") {
            supabaseClient.from(Constants.DB.TABLE_RESPONSE).insert(responses)
        }
    }

    /**
     * Upload locally cached MicroEMA responses to Supabase.
     * Handles timestamp formatting and batch insertion.
     * @param microEmaResponseDao DAO to access local Room database for responses.
     * @return Result containing the count of successfully uploaded responses.
     */
    suspend fun uploadUnsyncedMicroEmaResponses(
        microEmaResponseDao: MicroEmaResponseStore
    ): Result<Int> {
        return try {
            val unsynced = microEmaResponseDao.getUnsyncedResponses()
            if (unsynced.isEmpty()) return Result.Success(0)

            val inserts = unsynced.map { entity ->
                val fallbackTime = DateTimeFormatter.formatToIsoOffset(System.currentTimeMillis())
                SurveyQuestionResponseInsert(
                    questionId = entity.questionId,
                    uuid = entity.uuid,
                    triggerTime = DateTimeFormatter.formatToIsoOffset(entity.triggerTime)
                        ?: fallbackTime,
                    actualTriggerTime = DateTimeFormatter.formatToIsoOffset(entity.actualTriggerTime)
                        ?: fallbackTime,
                    surveyStartTime = DateTimeFormatter.formatToIsoOffset(entity.surveyStartTime)
                        ?: fallbackTime,
                    responseSubmissionTime = DateTimeFormatter.formatToIsoOffset(entity.responseSubmissionTime)
                        ?: DateTimeFormatter.formatToIsoOffset(entity.actualTriggerTime)
                        ?: fallbackTime,
                    response = kotlinx.serialization.json.Json.parseToJsonElement(entity.responseJson).jsonObject
                )
            }

            when (val result = submitSurveyResponses(inserts)) {
                is Result.Success -> {
                    microEmaResponseDao.markAsSynced(unsynced.map { it.id })
                    Log.d(TAG, "Successfully uploaded ${unsynced.size} MicroEMA responses")
                    Result.Success(unsynced.size)
                }

                is Result.Error -> Result.Error(
                    Exception(
                        "Failed to upload: ${result.message}",
                        result.exception
                    )
                )
            }
        } catch (e: Exception) {
            Result.Error(Exception("Error processing MicroEMA upload: ${e.message}", e))
        }
    }
}
