package kaist.iclab.mobiletracker.services


import io.github.jan.supabase.postgrest.from
import kaist.iclab.mobiletracker.Constants
import kaist.iclab.mobiletracker.data.survey.SurveyEntity
import kaist.iclab.mobiletracker.data.survey.SurveyQuestionEntity
import kaist.iclab.mobiletracker.data.survey.SurveyQuestionTriggerEntity
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.repository.ErrorClassifier
import kaist.iclab.mobiletracker.repository.Result
import kaist.iclab.mobiletracker.utils.SupabaseLoadingInterceptor
import kaist.iclab.tracker.sensor.survey.config.QuestionConfig
import kaist.iclab.tracker.sensor.survey.config.ScheduleType
import kaist.iclab.tracker.sensor.survey.config.SurveyConfig
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

/**
 * Service for fetching survey configuration from Supabase
 */
class SurveyService(
    supabaseHelper: SupabaseHelper
) {
    private val supabaseClient = supabaseHelper.supabaseClient

    companion object {
        /** Must match QUESTION_POSITION_KEY in the dashboard's campaignService.ts. */
        private const val QUESTION_POSITION_KEY = "position"
        private const val TAG = "SurveyService"
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
            // survey_question has no order column: the dashboard stores each question's index
            // among its siblings in config.position (older rows have none and fall back to id).
            // Builders keep this list order, so it sets the order on both phone and watch.
            val surveyQuestions = questions
                .filter { it.surveyId == survey.id }
                .sortedWith(compareBy({ it.config.int(QUESTION_POSITION_KEY) ?: Int.MAX_VALUE }, { it.id }))
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
}
