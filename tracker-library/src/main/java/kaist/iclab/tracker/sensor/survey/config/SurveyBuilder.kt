package kaist.iclab.tracker.sensor.survey.config

import kaist.iclab.tracker.sensor.survey.Survey
import kaist.iclab.tracker.sensor.survey.SurveyNotificationConfig
import kaist.iclab.tracker.sensor.survey.SurveyScheduleMethod
import kaist.iclab.tracker.sensor.survey.question.BinaryQuestion
import kaist.iclab.tracker.sensor.survey.question.ComparablePredicate
import kaist.iclab.tracker.sensor.survey.question.Expression
import kaist.iclab.tracker.sensor.survey.question.MultipleSelectionQuestion
import kaist.iclab.tracker.sensor.survey.question.NumberQuestion
import kaist.iclab.tracker.sensor.survey.question.NumberScaleQuestion
import kaist.iclab.tracker.sensor.survey.question.Predicate
import kaist.iclab.tracker.sensor.survey.question.Question
import kaist.iclab.tracker.sensor.survey.question.QuestionTrigger
import kaist.iclab.tracker.sensor.survey.question.SetPredicate
import kaist.iclab.tracker.sensor.survey.question.SingleSelectionQuestion
import kaist.iclab.tracker.sensor.survey.question.StringPredicate
import kaist.iclab.tracker.sensor.survey.question.TextQuestion
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.concurrent.TimeUnit

/**
 * Builds runtime Survey objects from serializable SurveyConfig.
 */
object SurveyBuilder {

    /**
     * Build a Survey object from a configuration.
     */
    fun build(config: SurveyConfig): Survey {
        val scheduleMethod = buildSchedule(config.scheduleType, config.schedule)
        val notificationConfig = SurveyNotificationConfig(
            title = config.title,
            description = config.description ?: "",
            icon = android.R.drawable.ic_menu_edit // Default icon
        )

        // Group questions by parentId to reconstruct the hierarchy
        val parentIdMap = config.questions.groupBy { it.parentId }

        // Start building from root questions (those with null parentId)
        val rootQuestions = parentIdMap[null]?.mapNotNull { buildQuestion(it, parentIdMap) } ?: emptyList()

        return Survey(
            id = config.id.toString(),
            scheduleMethod = scheduleMethod,
            notificationConfig = notificationConfig,
            *rootQuestions.toTypedArray()
        )
    }

    private fun buildSchedule(type: String, jsonStr: String?): SurveyScheduleMethod {
        val json = try {
            jsonStr?.let { Json.parseToJsonElement(it).jsonObject }
        } catch (_: Exception) {
            null
        }

        return when (type.uppercase()) {
            "TIME_OF_DAY" -> {
                val timesArray = json?.get("timeOfDay") as? JsonArray
                val times = timesArray?.mapNotNull { parseTime(it.jsonPrimitive.content) }
                    ?: listOf(TimeUnit.HOURS.toMillis(12))
                SurveyScheduleMethod.Fixed(timeOfDay = times)
            }

            "ESM" -> {
                if (json != null) {
                    SurveyScheduleMethod.ESM(
                        minInterval = json["minInterval"]?.jsonPrimitive?.content?.toLongOrNull() ?: 3600000L,
                        maxInterval = json["maxInterval"]?.jsonPrimitive?.content?.toLongOrNull() ?: 7200000L,
                        startOfDay = json["startOfDay"]?.jsonPrimitive?.content?.toLongOrNull() ?: 28800000L,
                        endOfDay = json["endOfDay"]?.jsonPrimitive?.content?.toLongOrNull() ?: 79200000L,
                        numSurvey = json["numSurvey"]?.jsonPrimitive?.content?.toIntOrNull() ?: 3
                    )
                } else {
                    SurveyScheduleMethod.Manual()
                }
            }

            else -> SurveyScheduleMethod.Manual()
        }
    }

    private fun parseTime(timeStr: String): Long? {
        return try {
            if (timeStr.contains(":")) {
                val parts = timeStr.split(":")
                val hours = parts[0].toLongOrNull() ?: 12
                val minutes = parts.getOrNull(1)?.toLongOrNull() ?: 0
                TimeUnit.HOURS.toMillis(hours) + TimeUnit.MINUTES.toMillis(minutes)
            } else {
                timeStr.toLongOrNull() ?: TimeUnit.HOURS.toMillis(12)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun buildQuestion(
        config: QuestionConfig,
        parentIdMap: Map<Int?, List<QuestionConfig>>
    ): Question<*>? {
        // Recursively build children questions
        val childrenQuestions = parentIdMap[config.id]?.mapNotNull { childConfig ->
            buildTriggeredQuestion(config.type, childConfig, parentIdMap)
        } ?: emptyList()

        return try {
            @Suppress("UNCHECKED_CAST")
            when (config.type.uppercase()) {
                "TEXT" -> TextQuestion(
                    id = config.id,
                    question = config.text,
                    isMandatory = config.isMandatory,
                    questionTrigger = childrenQuestions as? List<QuestionTrigger<String>>
                )

                "RADIO" -> {
                    SingleSelectionQuestion(
                        id = config.id,
                        question = config.text,
                        isMandatory = config.isMandatory,
                        option = config.options ?: listOf(),
                        allowFreeResponse = config.allowFreeResponse,
                        freeResponsePrefix = config.freeResponsePrefix?.takeIf { it.isNotBlank() } ?: "Other: ",
                        questionTrigger = childrenQuestions as? List<QuestionTrigger<Int?>>
                    )
                }

                "BINARY" -> {
                    BinaryQuestion(
                        id = config.id,
                        question = config.text,
                        isMandatory = config.isMandatory,
                        questionTrigger = childrenQuestions as? List<QuestionTrigger<Boolean?>>
                    )
                }

                "CHECKBOX" -> {
                    MultipleSelectionQuestion(
                        id = config.id,
                        question = config.text,
                        isMandatory = config.isMandatory,
                        option = config.options ?: listOf(),
                        allowFreeResponse = config.allowFreeResponse,
                        freeResponsePrefix = config.freeResponsePrefix?.takeIf { it.isNotBlank() } ?: "Other: ",
                        questionTrigger = childrenQuestions as? List<QuestionTrigger<Set<Int>>>
                    )
                }

                "NUMBER" -> NumberQuestion(
                    id = config.id,
                    question = config.text,
                    isMandatory = config.isMandatory,
                    questionTrigger = childrenQuestions as? List<QuestionTrigger<Double?>>
                )

                "NUMBERSCALE" -> {
                    NumberScaleQuestion(
                        id = config.id,
                        question = config.text,
                        isMandatory = config.isMandatory,
                        min = config.min ?: 0,
                        max = config.max ?: 10,
                        minLabel = config.minLabel!!,
                        maxLabel = config.maxLabel!!,
                        questionTrigger = childrenQuestions as? List<QuestionTrigger<Int?>>
                    )
                }

                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun buildTriggeredQuestion(
        parentType: String,
        config: QuestionConfig,
        parentIdMap: Map<Int?, List<QuestionConfig>>
    ): QuestionTrigger<*>? {
        val triggerJson = try {
            config.trigger?.let { Json.parseToJsonElement(it).jsonObject }
        } catch (_: Exception) {
            null
        } ?: return null

        val op = triggerJson["op"]?.jsonPrimitive?.content ?: return null
        val value = triggerJson["value"]

        val expression = parseExpression(op, value, parentType) ?: return null
        val question = buildQuestion(config, parentIdMap) ?: return null

        return when (parentType.uppercase()) {
            "TEXT" -> QuestionTrigger(expression as Expression<String>, listOf(question))
            "NUMBER", "NUMBERSCALE" -> QuestionTrigger(expression as Expression<Double?>, listOf(question))
            "RADIO", "BINARY" -> QuestionTrigger(expression as Expression<Int?>, listOf(question))
            "CHECKBOX" -> QuestionTrigger(expression as Expression<Set<Int>>, listOf(question))
            else -> null
        }
    }

    private fun parseExpression(
        op: String,
        value: JsonElement?,
        parentType: String
    ): Expression<*>? {
        return when (parentType.uppercase()) {
            "TEXT" -> parseTextExpression(op, value)
            "NUMBER", "NUMBERSCALE" -> parseNumberExpression(op, value)
            "RADIO", "BINARY" -> parseRadioExpression(op, value)
            "CHECKBOX" -> parseCheckboxExpression(op, value)
            else -> null
        }
    }

    private fun parseTextExpression(op: String, value: JsonElement?): Expression<String>? {
        return when (op) {
            "Equal" -> Predicate.Equal(value?.jsonPrimitive?.content ?: "")
            "NotEqual" -> Predicate.NotEqual(value?.jsonPrimitive?.content ?: "")
            "Empty" -> StringPredicate.Empty()
            else -> null
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseNumberExpression(op: String, value: JsonElement?): Expression<Double?>? {
        val target = value?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
        return when (op) {
            "Equal" -> Predicate.Equal(target as Double?)
            "NotEqual" -> Predicate.NotEqual(target as Double?)
            "GreaterThan" -> ComparablePredicate.GreaterThan(target)
            "GreaterThanOrEqual" -> ComparablePredicate.GreaterThanOrEqual(target)
            "LessThan" -> ComparablePredicate.LessThan(target)
            "LessThanOrEqual" -> ComparablePredicate.LessThanOrEqual(target)
            else -> null
        } as Expression<Double?>?
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseRadioExpression(op: String, value: JsonElement?): Expression<Int?>? {
        val target = try {
            value?.jsonPrimitive?.content?.toDoubleOrNull()?.toInt()
        } catch (_: Exception) {
            null
        }
        return when (op) {
            "Equal" -> Predicate.Equal(target)
            "NotEqual" -> Predicate.NotEqual(target)
            "GreaterThan" -> target?.let { ComparablePredicate.GreaterThan(it) }
            "GreaterThanOrEqual" -> target?.let { ComparablePredicate.GreaterThanOrEqual(it) }
            "LessThan" -> target?.let { ComparablePredicate.LessThan(it) }
            "LessThanOrEqual" -> target?.let { ComparablePredicate.LessThanOrEqual(it) }
            else -> null
        } as Expression<Int?>?
    }

    private fun parseCheckboxExpression(op: String, value: JsonElement?): Expression<Set<Int>>? {
        return when (op) {
            "Equal" -> {
                val array = try {
                    if (value is JsonArray) {
                        Json.decodeFromJsonElement<List<Int>>(value)
                    } else if (value != null) {
                        listOf(value.jsonPrimitive.content.toDoubleOrNull()?.toInt() ?: 0)
                    } else emptyList()
                } catch (_: Exception) {
                    emptyList()
                }
                Predicate.Equal(array.toSet())
            }

            "Contains" -> {
                val target = value?.jsonPrimitive?.content?.toDoubleOrNull()?.toInt() ?: return null
                SetPredicate.Contains<Int, Set<Int>>(target)
            }

            else -> null
        }
    }
}
