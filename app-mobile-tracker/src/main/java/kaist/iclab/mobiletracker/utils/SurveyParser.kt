package kaist.iclab.mobiletracker.utils

import android.util.Log
import kaist.iclab.mobiletracker.R
import kaist.iclab.tracker.sensor.survey.Survey
import kaist.iclab.tracker.sensor.survey.SurveyNotificationConfig
import kaist.iclab.tracker.sensor.survey.SurveyScheduleMethod
import kaist.iclab.tracker.sensor.survey.question.CheckboxQuestion
import kaist.iclab.tracker.sensor.survey.question.Expression
import kaist.iclab.tracker.sensor.survey.question.NumberQuestion
import kaist.iclab.tracker.sensor.survey.question.Option
import kaist.iclab.tracker.sensor.survey.question.Question
import kaist.iclab.tracker.sensor.survey.question.QuestionTrigger
import kaist.iclab.tracker.sensor.survey.question.RadioQuestion
import kaist.iclab.tracker.sensor.survey.question.TextQuestion
import kaist.iclab.tracker.sensor.survey.question.ValueComparator
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object SurveyParser {
    private const val TAG = "SurveyParser"

    data class ParsedSurvey(
        val id: String,
        val survey: Survey,
        val scheduleMethod: SurveyScheduleMethod,
        val notificationConfig: SurveyNotificationConfig,
        val startTimeOfDay: Long,
        val endTimeOfDay: Long
    )

    fun parse(jsonString: String, defaultSurveyId: String = "user_survey"): ParsedSurvey {
        try {
            val root = JSONObject(jsonString)
            val surveyId = root.optString("id", defaultSurveyId)

            // Parse Schedule
            val scheduleJson = root.optJSONObject("schedule")
                ?: throw IllegalArgumentException("Missing 'schedule' configuration object")

            val type = scheduleJson.optString("type", "INTERVAL")
            val scheduleMethod = when (type) {
                "INTERVAL" -> {
                    val validIntervalSec = scheduleJson.optLong("intervalSec", 7200)
                    val intervalMillis = TimeUnit.SECONDS.toMillis(validIntervalSec)

                    val startTimeHour = scheduleJson.optInt("dailyStartTimeHour", 9)
                    val endTimeHour = scheduleJson.optInt("dailyEndTimeHour", 22)

                    // Calculate fixed times based on interval
                    val times = mutableListOf<Long>()

                    val startMillis = TimeUnit.HOURS.toMillis(startTimeHour.toLong())
                    var endMillis = TimeUnit.HOURS.toMillis(endTimeHour.toLong())

                    // Handle spanning midnight (e.g. 22:00 to 02:00)
                    if (endMillis < startMillis) {
                        endMillis += TimeUnit.DAYS.toMillis(1)
                    }

                    var current = startMillis
                    while (current <= endMillis) {
                        val timeOfDay = if (current >= TimeUnit.DAYS.toMillis(1)) {
                            current - TimeUnit.DAYS.toMillis(1)
                        } else {
                            current
                        }
                        times.add(timeOfDay)
                        current += intervalMillis
                    }
                    SurveyScheduleMethod.Fixed(timeOfDay = times)
                }

                "FIXED" -> {
                    val timesJson = scheduleJson.optJSONArray("times")
                    val times = mutableListOf<Long>()
                    if (timesJson != null) {
                        for (i in 0 until timesJson.length()) {
                            // Support "HH:mm" strings or integer hours?
                            // For simplicity, let's assume usage of explicit ms or we parse "HH:mm"
                            // Let's support easy "HH:mm" format as string, or simple integer hour.
                            val item = timesJson.get(i)
                            if (item is String && item.contains(":")) {
                                val parts = item.split(":")
                                if (parts.size == 2) {
                                    val h = parts[0].toIntOrNull() ?: 0
                                    val m = parts[1].toIntOrNull() ?: 0
                                    times.add(
                                        TimeUnit.HOURS.toMillis(h.toLong()) + TimeUnit.MINUTES.toMillis(
                                            m.toLong()
                                        )
                                    )
                                }
                            } else if (item is Int) {
                                times.add(TimeUnit.HOURS.toMillis(item.toLong()))
                            }
                        }
                    }
                    if (times.isEmpty()) {
                        // Fallback
                        times.add(TimeUnit.HOURS.toMillis(12))
                    }
                    SurveyScheduleMethod.Fixed(timeOfDay = times)
                }

                else -> {
                    SurveyScheduleMethod.Fixed(timeOfDay = listOf(TimeUnit.HOURS.toMillis(12)))
                }
            }

            val startTimeOfDay =
                TimeUnit.HOURS.toMillis(scheduleJson.optInt("dailyStartTimeHour", 9).toLong())
            val endTimeOfDay =
                TimeUnit.HOURS.toMillis(scheduleJson.optInt("dailyEndTimeHour", 22).toLong())

            // Parse Notification
            val notificationJson = root.optJSONObject("notification")
                ?: throw IllegalArgumentException("Missing 'notification' configuration object")
            
            val notificationConfig = SurveyNotificationConfig(
                title = notificationJson.optString("title", "Survey"),
                description = notificationJson.optString("message", "Please answer the survey"),
                icon = R.drawable.ic_launcher_foreground
            )

            // Parse Questions
            val questionsJson = root.getJSONArray("questions")
            val questions = parseQuestions(questionsJson)

            return ParsedSurvey(
                id = surveyId,
                survey = Survey(*questions.toTypedArray()),
                scheduleMethod = scheduleMethod,
                notificationConfig = notificationConfig,
                startTimeOfDay = startTimeOfDay,
                endTimeOfDay = endTimeOfDay
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse survey JSON", e)
            // Return a safe fallback to prevent crash
            return ParsedSurvey(
                id = "error_survey",
                survey = Survey(TextQuestion("Error loading survey configuration. Please check the logs.", false)),
                scheduleMethod = SurveyScheduleMethod.Fixed(listOf()),
                notificationConfig = SurveyNotificationConfig("Error", "Configuration Error", R.drawable.ic_launcher_foreground),
                startTimeOfDay = 0L,
                endTimeOfDay = 0L
            )
        }
    }

    private fun parseQuestions(jsonArray: JSONArray): List<Question<*>> {
        val questions = mutableListOf<Question<*>>()
        for (i in 0 until jsonArray.length()) {
            val qJson = jsonArray.getJSONObject(i)
            val question = parseQuestion(qJson)
            if (question != null) {
                questions.add(question)
            }
        }
        return questions
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseQuestion(json: JSONObject): Question<*>? {
        val type = json.getString("type")
        val text = json.getString("text")
        val isMandatory = json.optBoolean("shouldAnswer", true)

        // Default isNumeric = false
        val trigger = parseTrigger(json.optJSONObject("trigger"), isNumeric = false)

        return when (type) {
            "TEXT" -> TextQuestion(
                question = text,
                isMandatory = isMandatory,
                questionTrigger = trigger as? List<QuestionTrigger<String>>
            )

            "NUMBER" -> NumberQuestion(
                question = text,
                isMandatory = isMandatory,
                questionTrigger = parseTrigger(
                    json.optJSONObject("trigger"),
                    isNumeric = true
                ) as? List<QuestionTrigger<Double?>>
            )

            "RADIO" -> {
                val options = parseOptions(json.optJSONArray("options"))
                if (options.isEmpty()) {
                    Log.e(
                        TAG,
                        "Skipping RADIO question '$text': No options provided."
                    )
                    return null
                }
                RadioQuestion(
                    question = text,
                    isMandatory = isMandatory,
                    option = options,
                    questionTrigger = trigger as? List<QuestionTrigger<String>>
                )
            }

            "CHECKBOX" -> {
                val options = parseOptions(json.optJSONArray("options"))
                if (options.isEmpty()) {
                    Log.e(
                        TAG,
                        "Skipping CHECKBOX question '$text': No options provided."
                    )
                    return null
                }
                CheckboxQuestion(
                    question = text,
                    isMandatory = isMandatory,
                    option = options,
                    questionTrigger = trigger as? List<QuestionTrigger<Set<String>>>
                )
            }

            else -> {
                Log.w(TAG, "Unknown question type: $type")
                null
            }
        }
    }

    private fun parseOptions(jsonArray: JSONArray?): List<Option> {
        val options = mutableListOf<Option>()
        if (jsonArray == null || jsonArray.length() == 0) return options

        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.opt(i) // Use opt to be safe
            if (item is String) {
                options.add(Option(item))
            } else if (item is JSONObject) {
                val value = item.optString("value")
                if (value.isNotEmpty()) {
                    options.add(
                        Option(
                            value = value,
                            displayText = item.optString("displayText", value),
                            allowFreeResponse = item.optBoolean("allowFreeResponse", false)
                        )
                    )
                } else {
                    Log.w(TAG, "Skipping option with missing value")
                }
            }
        }
        return options
    }

    private fun parseTrigger(json: JSONObject?, isNumeric: Boolean): List<QuestionTrigger<*>>? {
        if (json == null) return null

        val predicateJson = json.getJSONObject("predicate")
        val predicateType = predicateJson.getString("type")
        val predicateValue = predicateJson.getString("value")

        // Helper to infer type for predicate
        // This is tricky because we don't know the parent question's type here easily without passing it down.
        // But the previous implementation logic implies we construct generic triggers.
        // For simple string equality (Radio/Text), we treat value as String.
        // For Number, we might need to parse.
        // For Checkbox, we might need "contains" logic (not implemented here yet).

        // Strategy: Create the specific typed predicate based on try-parse logic or assumption.
        // Since we are casting at the call site (e.g. `as? List<QuestionTrigger<String>>`), 
        // we need to make sure the runtime object matches.

        val predicate: ValueComparator<*> = when (predicateType) {
            "EQUAL" -> {
                if (isNumeric) {
                    val doubleVal = predicateJson.optDouble("value", Double.NaN)
                    if (!doubleVal.isNaN()) {
                        ValueComparator.Equal(doubleVal)
                    } else {
                        val stringVal = predicateJson.optString("value")
                        val parsed = stringVal.toDoubleOrNull() ?: return null
                        ValueComparator.Equal(parsed)
                    }
                } else {
                    ValueComparator.Equal(predicateValue)
                }
            }

            "NOT_EQUAL" -> {
                if (isNumeric) {
                    val doubleVal = predicateJson.optDouble("value", Double.NaN)
                    if (!doubleVal.isNaN()) {
                        ValueComparator.NotEqual(doubleVal)
                    } else {
                        val stringVal = predicateJson.optString("value")
                        val parsed = stringVal.toDoubleOrNull() ?: return null
                        ValueComparator.NotEqual(parsed)
                    }
                } else {
                    ValueComparator.NotEqual(predicateValue)
                }
            }

            "GREATER_THAN" -> {
                if (isNumeric) {
                    val doubleVal = predicateJson.optDouble("value", Double.NaN)
                    if (!doubleVal.isNaN()) {
                        ValueComparator.GreaterThan(doubleVal)
                    } else {
                        val stringVal = predicateJson.optString("value")
                        val parsed = stringVal.toDoubleOrNull() ?: return null
                        ValueComparator.GreaterThan(parsed)
                    }
                } else {
                    return null // String inequality not typically supported or useful here
                }
            }

            "GREATER_THAN_OR_EQUAL" -> {
                if (isNumeric) {
                    val doubleVal = predicateJson.optDouble("value", Double.NaN)
                    if (!doubleVal.isNaN()) {
                        ValueComparator.GreaterThanOrEqual(doubleVal)
                    } else {
                        val stringVal = predicateJson.optString("value")
                        val parsed = stringVal.toDoubleOrNull() ?: return null
                        ValueComparator.GreaterThanOrEqual(parsed)
                    }
                } else {
                    return null
                }
            }

            "LESS_THAN" -> {
                if (isNumeric) {
                    val doubleVal = predicateJson.optDouble("value", Double.NaN)
                    if (!doubleVal.isNaN()) {
                        ValueComparator.LessThan(doubleVal)
                    } else {
                        val stringVal = predicateJson.optString("value")
                        val parsed = stringVal.toDoubleOrNull() ?: return null
                        ValueComparator.LessThan(parsed)
                    }
                } else {
                    return null
                }
            }

            "LESS_THAN_OR_EQUAL" -> {
                if (isNumeric) {
                    val doubleVal = predicateJson.optDouble("value", Double.NaN)
                    if (!doubleVal.isNaN()) {
                        ValueComparator.LessThanOrEqual(doubleVal)
                    } else {
                        val stringVal = predicateJson.optString("value")
                        val parsed = stringVal.toDoubleOrNull() ?: return null
                        ValueComparator.LessThanOrEqual(parsed)
                    }
                } else {
                    return null
                }
            }

            else -> null
        } ?: return null

        val questionsJson = json.getJSONArray("questions")
        val children = parseQuestions(questionsJson)

        @Suppress("UNCHECKED_CAST")
        return listOf(QuestionTrigger(predicate as Expression<Any>, children))
    }
}
