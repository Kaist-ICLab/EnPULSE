package kaist.iclab.tracker.sensor.survey.config

import kaist.iclab.tracker.sensor.survey.Survey
import kaist.iclab.tracker.sensor.survey.SurveyNotificationConfig
import kaist.iclab.tracker.sensor.survey.SurveyScheduleMethod
import kaist.iclab.tracker.sensor.survey.question.CheckboxQuestion
import kaist.iclab.tracker.sensor.survey.question.ComparablePredicate
import kaist.iclab.tracker.sensor.survey.question.Expression
import kaist.iclab.tracker.sensor.survey.question.NumberQuestion
import kaist.iclab.tracker.sensor.survey.question.Option
import kaist.iclab.tracker.sensor.survey.question.Predicate
import kaist.iclab.tracker.sensor.survey.question.Question
import kaist.iclab.tracker.sensor.survey.question.QuestionTrigger
import kaist.iclab.tracker.sensor.survey.question.RadioQuestion
import kaist.iclab.tracker.sensor.survey.question.SetPredicate
import kaist.iclab.tracker.sensor.survey.question.StringPredicate
import kaist.iclab.tracker.sensor.survey.question.TextQuestion
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
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
        val scheduleMethod = buildSchedule(config.schedule)
        val notificationConfig = SurveyNotificationConfig(
            title = config.notification.title,
            description = config.notification.description,
            icon = android.R.drawable.ic_menu_edit // Default icon, can be customized later if needed
        )

        val questions = config.questions.mapNotNull { buildQuestion(it) }

        return Survey(
            scheduleMethod = scheduleMethod,
            notificationConfig = notificationConfig,
            *questions.toTypedArray()
        )
    }

    private fun buildSchedule(config: ScheduleConfig): SurveyScheduleMethod {
        return when (config.type.uppercase()) {
            "TIME_OF_DAY" -> {
                val times = config.timeOfDay?.mapNotNull { parseTime(it) }
                    ?: listOf(TimeUnit.HOURS.toMillis(12))
                SurveyScheduleMethod.Fixed(timeOfDay = times)
            }

            "ESM" -> {
                val esm = config.esm
                if (esm != null) {
                    SurveyScheduleMethod.ESM(
                        minInterval = esm.minInterval,
                        maxInterval = esm.maxInterval,
                        startOfDay = esm.startOfDay,
                        endOfDay = esm.endOfDay,
                        numSurvey = esm.numSurvey
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
        } catch (e: Exception) {
            null
        }
    }

    private fun buildQuestion(config: QuestionConfig): Question<*>? {
        val childrenQuestions = config.children?.mapNotNull { childConfig ->
            buildTrigger(config.type, childConfig)
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

                "RADIO", "BINARY" -> {
                    val options = config.options?.map { Option(it.display, it.allowFreeResponse) }
                        ?: emptyList()
                    if (options.isEmpty()) return null
                    RadioQuestion(
                        id = config.id,
                        question = config.text,
                        isMandatory = config.isMandatory,
                        option = options,
                        questionTrigger = childrenQuestions as? List<QuestionTrigger<Int?>>
                    )
                }

                "CHECKBOX" -> {
                    val options = config.options?.map { Option(it.display, it.allowFreeResponse) }
                        ?: emptyList()
                    if (options.isEmpty()) return null
                    CheckboxQuestion(
                        id = config.id,
                        question = config.text,
                        isMandatory = config.isMandatory,
                        option = options,
                        questionTrigger = childrenQuestions as? List<QuestionTrigger<Set<Int>>>
                    )
                }

                "NUMBER", "NUMBERSCALE" -> NumberQuestion(
                    id = config.id,
                    question = config.text,
                    isMandatory = config.isMandatory,
                    questionTrigger = childrenQuestions as? List<QuestionTrigger<Double?>>
                )

                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun buildTrigger(parentType: String, config: ChildQuestionConfig): QuestionTrigger<*>? {
        val expression =
            parseExpression(config.trigger.op, config.trigger.value, parentType) ?: return null
        val questions = config.questions.mapNotNull { buildQuestion(it) }

        if (questions.isEmpty()) return null

        return when (parentType.uppercase()) {
            "TEXT" -> QuestionTrigger(expression as Expression<String>, questions)
            "NUMBER", "NUMBERSCALE" -> QuestionTrigger(expression as Expression<Double?>, questions)
            "RADIO", "BINARY" -> QuestionTrigger(expression as Expression<Int?>, questions)
            "CHECKBOX" -> QuestionTrigger(expression as Expression<Set<Int>>, questions)
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
        } catch (e: Exception) {
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
                } catch (e: Exception) {
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
