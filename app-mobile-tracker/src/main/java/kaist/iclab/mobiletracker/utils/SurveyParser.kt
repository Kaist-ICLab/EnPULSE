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
                    if (validIntervalSec <= 0) throw IllegalArgumentException("intervalSec must be positive")
                    val intervalMillis = TimeUnit.SECONDS.toMillis(validIntervalSec)

                    val startTimeHour = scheduleJson.optInt("dailyStartTimeHour", 9)
                    val endTimeHour = scheduleJson.optInt("dailyEndTimeHour", 22)
                    
                    if (startTimeHour !in 0..23 || endTimeHour !in 0..23) {
                         throw IllegalArgumentException("Daily hours must be between 0 and 23")
                    }

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
        val type = json.getString("type").uppercase()
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

    private fun parseOptions(jsonArray: JSONArray?): List<Option> {
        val options = mutableListOf<Option>()
        val seenValues = mutableSetOf<String>()
        if (jsonArray == null || jsonArray.length() == 0) return options

        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.opt(i) // Use opt to be safe
            if (item is String) {
                if (!seenValues.add(item)) throw IllegalArgumentException("Duplicate option value: $item")
                options.add(Option(item))
            } else if (item is JSONObject) {
                val value = item.optString("value")
                if (value.isNotEmpty()) {
                    if (!seenValues.add(value)) throw IllegalArgumentException("Duplicate option value: $value")
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
        val predicateType = predicateJson.getString("type").uppercase()
        // Removed strict getString("value") here to allow flexible parsing below
        
        val predicate: ValueComparator<*> = when (predicateType) {
            "EQUAL" -> {
                if (isNumeric) {
                    val doubleVal = predicateJson.optDouble("value", Double.NaN)
                    if (!doubleVal.isNaN()) {
                        ValueComparator.Equal(doubleVal)
                    } else {
                        val stringVal = predicateJson.getString("value")
                        val parsed = stringVal.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid numeric value for EQUAL: $stringVal")
                        ValueComparator.Equal(parsed)
                    }
                } else {
                    ValueComparator.Equal(predicateJson.getString("value"))
                }
            }

            "NOT_EQUAL" -> {
                if (isNumeric) {
                     val doubleVal = predicateJson.optDouble("value", Double.NaN)
                    if (!doubleVal.isNaN()) {
                        ValueComparator.NotEqual(doubleVal)
                    } else {
                        val stringVal = predicateJson.getString("value")
                        val parsed = stringVal.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid numeric value for NOT_EQUAL: $stringVal")
                        ValueComparator.NotEqual(parsed)
                    }
                } else {
                    ValueComparator.NotEqual(predicateJson.getString("value"))
                }
            }
            
            // ... (Rest of inequality logic handled same way, but access value directly)
            "GREATER_THAN" -> {
                 if (isNumeric) {
                    val doubleVal = predicateJson.optDouble("value", Double.NaN)
                    val value = if (!doubleVal.isNaN()) doubleVal else predicateJson.getString("value").toDoubleOrNull() ?: throw IllegalArgumentException("Invalid numeric value")
                    ValueComparator.GreaterThan(value)
                 } else null
            }
            "GREATER_THAN_OR_EQUAL" -> {
                 if (isNumeric) {
                    val doubleVal = predicateJson.optDouble("value", Double.NaN)
                    val value = if (!doubleVal.isNaN()) doubleVal else predicateJson.getString("value").toDoubleOrNull() ?: throw IllegalArgumentException("Invalid numeric value")
                    ValueComparator.GreaterThanOrEqual(value)
                 } else null
            }
            "LESS_THAN" -> {
                 if (isNumeric) {
                    val doubleVal = predicateJson.optDouble("value", Double.NaN)
                    val value = if (!doubleVal.isNaN()) doubleVal else predicateJson.getString("value").toDoubleOrNull() ?: throw IllegalArgumentException("Invalid numeric value")
                    ValueComparator.LessThan(value)
                 } else null
            }
            "LESS_THAN_OR_EQUAL" -> {
                 if (isNumeric) {
                    val doubleVal = predicateJson.optDouble("value", Double.NaN)
                    val value = if (!doubleVal.isNaN()) doubleVal else predicateJson.getString("value").toDoubleOrNull() ?: throw IllegalArgumentException("Invalid numeric value")
                    ValueComparator.LessThanOrEqual(value)
                 } else null
            }

            else -> null
        } ?: return null

        val questionsJson = json.getJSONArray("questions")
        val children = parseQuestions(questionsJson)

        @Suppress("UNCHECKED_CAST")
        return listOf(QuestionTrigger(predicate as Expression<Any>, children))
    }
}
