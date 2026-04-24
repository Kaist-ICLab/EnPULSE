package kaist.iclab.mobiletracker.services


import android.util.Log
import io.github.jan.supabase.postgrest.from
import kaist.iclab.mobiletracker.Constants
import kaist.iclab.mobiletracker.data.survey.OptionConfig
import kaist.iclab.mobiletracker.data.survey.QuestionConfig
import kaist.iclab.mobiletracker.data.survey.ScheduleType
import kaist.iclab.mobiletracker.data.survey.SurveyConfig
import kaist.iclab.mobiletracker.data.survey.SurveyEntity
import kaist.iclab.mobiletracker.data.survey.SurveyQuestionEntity
import kaist.iclab.mobiletracker.data.survey.SurveyQuestionOptionEntity
import kaist.iclab.mobiletracker.data.survey.SurveyQuestionResponseInsert
import kaist.iclab.mobiletracker.data.survey.SurveyQuestionTriggerEntity
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.repository.ErrorClassifier
import kaist.iclab.mobiletracker.repository.Result
import kaist.iclab.mobiletracker.repository.runCatchingSuspend
import kaist.iclab.mobiletracker.utils.SupabaseLoadingInterceptor
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.time.Instant
import java.time.ZoneOffset

/**
 * Service for fetching survey configuration from Supabase
 */
class SurveyService(
    private val supabaseHelper: SupabaseHelper
) {
    private val supabaseClient = supabaseHelper.supabaseClient

    companion object {
        private const val TAG = "SurveyService"

    }

    /**
     * Fetch a single survey configuration by ID
     * @param surveyId The survey ID
     * @return Result containing the assembled SurveyConfig or error
     */
    suspend fun getSurveyConfig(surveyId: Int): Result<SurveyConfig> {
        return SupabaseLoadingInterceptor.withLoading {
            runCatchingSuspend {
                // 1. Fetch survey
                val survey = supabaseClient.from(Constants.DB.TABLE_SURVEY)
                    .select {
                        filter {
                            eq("id", surveyId)
                        }
                    }
                    .decodeSingleOrNull<SurveyEntity>()
                    ?: throw NoSuchElementException("Survey with ID $surveyId not found")

                // 2. Fetch questions
                val questions = supabaseClient.from(Constants.DB.TABLE_QUESTION)
                    .select {
                        filter {
                            eq("survey_id", surveyId)
                        }
                    }
                    .decodeList<SurveyQuestionEntity>()

                // 3. Fetch options for all questions
                val questionIds = questions.map { q -> q.id }
                val options = if (questionIds.isNotEmpty()) {
                    supabaseClient.from(Constants.DB.TABLE_OPTION)
                        .select {
                            filter {
                                isIn("question_id", questionIds)
                            }
                        }
                        .decodeList<SurveyQuestionOptionEntity>()
                } else emptyList()

                // 4. Fetch triggers
                val triggerIds = questions.mapNotNull { q -> q.triggeredBy }
                val triggers = if (triggerIds.isNotEmpty()) {
                    supabaseClient.from(Constants.DB.TABLE_TRIGGER)
                        .select {
                            filter {
                                isIn("id", triggerIds)
                            }
                        }
                        .decodeList<SurveyQuestionTriggerEntity>()
                } else emptyList()

                // 5. Build the config
                assembleConfig(survey, questions, options, triggers)
            }
        }
    }

    /**
     * Fetch all surveys for a campaign
     * @param campaignId The campaign ID
     * @return Result containing list of SurveyConfig or error
     */
    suspend fun getCampaignSurveys(campaignId: Int): Result<List<SurveyConfig>> {
        return SupabaseLoadingInterceptor.withLoading {
            ErrorClassifier.runClassified(TAG, "getCampaignSurveys($campaignId)") {
                // 1. Fetch all surveys for campaign
                val surveys = supabaseClient.from(Constants.DB.TABLE_SURVEY)
                    .select {
                        filter {
                            eq("campaign_id", campaignId)
                        }
                    }
                    .decodeList<SurveyEntity>()

                if (surveys.isEmpty()) {
                    return@runClassified emptyList()
                }

                val surveyIds = surveys.map { s -> s.id }

                // 2. Fetch all questions for these surveys
                val questions = supabaseClient.from(Constants.DB.TABLE_QUESTION)
                    .select {
                        filter {
                            isIn("survey_id", surveyIds)
                        }
                    }
                    .decodeList<SurveyQuestionEntity>()

                // 3. Fetch all options for these questions
                val questionIds = questions.map { q -> q.id }
                val options = if (questionIds.isNotEmpty()) {
                    supabaseClient.from(Constants.DB.TABLE_OPTION)
                        .select {
                            filter {
                                isIn("question_id", questionIds)
                            }
                        }
                        .decodeList<SurveyQuestionOptionEntity>()
                } else emptyList()

                // 4. Fetch all triggers
                val triggerIds = questions.mapNotNull { q -> q.triggeredBy }
                val triggers = if (triggerIds.isNotEmpty()) {
                    supabaseClient.from(Constants.DB.TABLE_TRIGGER)
                        .select {
                            filter {
                                isIn("id", triggerIds)
                            }
                        }
                        .decodeList<SurveyQuestionTriggerEntity>()
                } else emptyList()

                // 5. Assemble each survey
                surveys.map { survey ->
                    val surveyQuestions = questions.filter { q -> q.surveyId == survey.id }
                    val surveyQuestionIds = surveyQuestions.map { q -> q.id }
                    val surveyOptions =
                        options.filter { o -> o.questionId in surveyQuestionIds }
                    val surveyTriggerIds = surveyQuestions.mapNotNull { q -> q.triggeredBy }
                    val surveyTriggers = triggers.filter { t -> t.id in surveyTriggerIds }

                    assembleConfig(survey, surveyQuestions, surveyOptions, surveyTriggers)
                }
            }
        }
    }

    /**
     * Assemble SurveyConfig from entities
     */
    private fun assembleConfig(
        survey: SurveyEntity,
        questions: List<SurveyQuestionEntity>,
        options: List<SurveyQuestionOptionEntity>,
        triggers: List<SurveyQuestionTriggerEntity>
    ): SurveyConfig {
        val triggerMap = triggers.associateBy { t -> t.id }
        val scheduleType = getScheduleType(survey.scheduleMethod)

        return SurveyConfig(
            id = survey.id,
            campaignId = survey.campaignId,
            title = survey.title,
            description = survey.description,
            scheduleType = scheduleType,
            schedule = survey.scheduleMethod?.toString(),  // Convert JsonObject to String
            questions = questions.map { q ->
                val trigger = triggerMap[q.triggeredBy]
                QuestionConfig(
                    id = q.id,
                    parentId = trigger?.questionId,
                    type = q.answerType.uppercase(),
                    text = q.question,
                    shouldAnswer = q.isMandatory,
                    trigger = trigger?.expression?.toString(),  // Convert JsonObject to String
                    options = options
                        .filter { o -> o.questionId == q.id }
                        .map { o ->
                            OptionConfig(
                                id = o.id,
                                display = o.display,
                                allowFreeResponse = o.allowFreeResponse
                            )
                        }
                        .ifEmpty { null }
                )
            }
        )
    }

    /**
     * Determine schedule type from schedule_method JSON
     */
    private fun getScheduleType(scheduleMethod: JsonObject?): String {
        if (scheduleMethod == null) return ScheduleType.MANUAL
        if (scheduleMethod.containsKey("timeOfDay")) return ScheduleType.TIME_OF_DAY
        if (scheduleMethod.containsKey("numSurvey")) return ScheduleType.ESM
        return ScheduleType.MANUAL
    }

    /**
     * Submit survey question responses to Supabase
     * @param responses List of response entities to insert
     * @return Result indicating success or failure
     */
    suspend fun submitSurveyResponses(responses: List<SurveyQuestionResponseInsert>): Result<Unit> {
        return ErrorClassifier.runClassified(TAG, "submitSurveyResponses") {
            supabaseClient.from(Constants.DB.TABLE_RESPONSE).insert(responses)
        }
    }

    /**
     * Upload locally cached MicroEMA responses to Supabase.
     */
    suspend fun uploadUnsyncedMicroEmaResponses(
        microEmaResponseDao: kaist.iclab.mobiletracker.db.dao.phone.MicroEmaResponseDao
    ): Result<Int> {
        return try {
            val unsynced = microEmaResponseDao.getUnsyncedResponses()
            if (unsynced.isEmpty()) return Result.Success(0)

            val isoFormatter = java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME
            val inserts = unsynced.map { entity ->
                fun formatTimestamp(millisStr: String?) = millisStr?.toLongOrNull()?.let {
                    java.time.Instant.ofEpochMilli(it).atOffset(java.time.ZoneOffset.UTC)
                        .format(isoFormatter)
                }

                SurveyQuestionResponseInsert(
                    questionId = entity.questionId,
                    uuid = entity.uuid,
                    triggerTime = formatTimestamp(entity.triggerTime),
                    actualTriggerTime = formatTimestamp(entity.actualTriggerTime),
                    surveyStartTime = formatTimestamp(entity.surveyStartTime),
                    responseSubmissionTime = formatTimestamp(entity.responseSubmissionTime)
                        ?: formatTimestamp(entity.actualTriggerTime)
                        ?: Instant.now().atOffset(ZoneOffset.UTC).format(isoFormatter),
                    response = kotlinx.serialization.json.Json.parseToJsonElement(entity.responseJson).jsonObject,
                    deviceType = entity.deviceType
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
