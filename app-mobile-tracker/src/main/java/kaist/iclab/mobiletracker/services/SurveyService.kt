package kaist.iclab.mobiletracker.services


import android.util.Log
import io.github.jan.supabase.postgrest.from
import kaist.iclab.mobiletracker.Constants
import kaist.iclab.mobiletracker.data.survey.SurveyEntity
import kaist.iclab.mobiletracker.data.survey.SurveyQuestionEntity
import kaist.iclab.mobiletracker.data.survey.SurveyQuestionOptionEntity
import kaist.iclab.mobiletracker.data.survey.SurveyQuestionResponseInsert
import kaist.iclab.mobiletracker.data.survey.SurveyQuestionTriggerEntity
import kaist.iclab.mobiletracker.db.dao.phone.MicroEmaResponseDao
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.repository.ErrorClassifier
import kaist.iclab.mobiletracker.repository.Result
import kaist.iclab.mobiletracker.repository.runCatchingSuspend
import kaist.iclab.mobiletracker.utils.DateTimeFormatter
import kaist.iclab.mobiletracker.utils.SupabaseLoadingInterceptor
import kaist.iclab.tracker.sensor.survey.config.OptionConfig
import kaist.iclab.tracker.sensor.survey.config.QuestionConfig
import kaist.iclab.tracker.sensor.survey.config.ScheduleType
import kaist.iclab.tracker.sensor.survey.config.SurveyConfig
import kotlinx.serialization.json.JsonObject
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

        val questionIds = questions.map { it.id }

        // 3. Bulk fetch all options for the identified questions
        val options = if (questionIds.isNotEmpty()) {
            supabaseClient.from(Constants.DB.TABLE_OPTION)
                .select { filter { isIn("question_id", questionIds) } }
                .decodeList<SurveyQuestionOptionEntity>()
        } else emptyList()

        // 4. Bulk fetch all conditional triggers associated with the questions
        val triggerIds = questions.mapNotNull { it.triggeredBy }
        val triggers = if (triggerIds.isNotEmpty()) {
            supabaseClient.from(Constants.DB.TABLE_TRIGGER)
                .select { filter { isIn("id", triggerIds) } }
                .decodeList<SurveyQuestionTriggerEntity>()
        } else emptyList()

        // 5. Assemble the flat entities into a hierarchical configuration structure
        return surveys.map { survey ->
            val surveyQuestions = questions.filter { it.surveyId == survey.id }
            val surveyQuestionIds = surveyQuestions.map { it.id }
            val surveyOptions = options.filter { it.questionId in surveyQuestionIds }
            val surveyTriggerIds = surveyQuestions.mapNotNull { it.triggeredBy }
            val surveyTriggers = triggers.filter { it.id in surveyTriggerIds }

            assembleConfig(survey, surveyQuestions, surveyOptions, surveyTriggers)
        }
    }

    /**
     * Maps flat database entities into the SurveyConfig domain model.
     * Handles trigger mapping and hierarchical question structure.
     */
    private fun assembleConfig(
        survey: SurveyEntity,
        questions: List<SurveyQuestionEntity>,
        options: List<SurveyQuestionOptionEntity>,
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
                QuestionConfig(
                    id = q.id,
                    parentId = trigger?.questionId,
                    type = q.answerType.uppercase(),
                    text = q.question,
                    isMandatory = q.isMandatory,
                    trigger = trigger?.expression?.toString(),
                    options = options.filter { it.questionId == q.id }
                        .map { it.toConfig() }
                        .ifEmpty { null }
                )
            }
        )
    }

    /**
     * Extension to convert database Option entity to UI Config model.
     */
    private fun SurveyQuestionOptionEntity.toConfig() = OptionConfig(
        id = id,
        display = display,
        allowFreeResponse = allowFreeResponse
    )

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
        microEmaResponseDao: MicroEmaResponseDao
    ): Result<Int> {
        return try {
            val unsynced = microEmaResponseDao.getUnsyncedResponses()
            if (unsynced.isEmpty()) return Result.Success(0)

            val inserts = unsynced.map { entity ->
                SurveyQuestionResponseInsert(
                    questionId = entity.questionId,
                    uuid = entity.uuid,
                    triggerTime = DateTimeFormatter.formatToIsoOffset(entity.triggerTime),
                    actualTriggerTime = DateTimeFormatter.formatToIsoOffset(entity.actualTriggerTime),
                    surveyStartTime = DateTimeFormatter.formatToIsoOffset(entity.surveyStartTime),
                    responseSubmissionTime = DateTimeFormatter.formatToIsoOffset(entity.responseSubmissionTime)
                        ?: DateTimeFormatter.formatToIsoOffset(entity.actualTriggerTime)
                        ?: DateTimeFormatter.formatToIsoOffset(System.currentTimeMillis()),
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
