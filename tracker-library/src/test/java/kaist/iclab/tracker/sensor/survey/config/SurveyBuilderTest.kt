package kaist.iclab.tracker.sensor.survey.config

import kaist.iclab.tracker.sensor.survey.SurveyScheduleMethod
import kaist.iclab.tracker.sensor.survey.question.MultipleSelectionQuestion
import kaist.iclab.tracker.sensor.survey.question.NumberQuestion
import kaist.iclab.tracker.sensor.survey.question.SingleSelectionQuestion
import kaist.iclab.tracker.sensor.survey.question.TextQuestion
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Unit tests for [SurveyBuilder].
 *
 * Tests cover:
 * - Schedule building (MANUAL, TIME_OF_DAY, ESM)
 * - Time string parsing
 * - Question construction for all 4 question types
 * - Edge cases (empty options, unknown types, null children)
 */
class SurveyBuilderTest {

    // ========== Helper factories ==========

    private fun minimalConfig(
        scheduleType: String = "MANUAL",
        questions: List<QuestionConfig> = emptyList(),
        esm: EsmConfig? = null,
        timeOfDay: List<String>? = null
    ) = SurveyConfig(
        id = "test-survey",
        schedule = ScheduleConfig(type = scheduleType, esm = esm, timeOfDay = timeOfDay),
        notification = NotificationConfig(title = "Test", description = "A test survey"),
        questions = questions
    )

    private fun textQuestion(id: Int = 1, text: String = "What?") = QuestionConfig(
        id = id, type = "TEXT", text = text, isMandatory = true
    )

    private fun numberQuestion(id: Int = 2, text: String = "How many?") = QuestionConfig(
        id = id, type = "NUMBER", text = text, isMandatory = false
    )

    private fun radioQuestion(
        id: Int = 3, text: String = "Pick one",
        options: List<OptionConfig> = listOf(
            OptionConfig("Option A"),
            OptionConfig("Option B"),
            OptionConfig("Other", allowFreeResponse = true)
        )
    ) = QuestionConfig(
        id = id, type = "RADIO", text = text, isMandatory = true, options = options
    )

    private fun checkboxQuestion(
        id: Int = 4, text: String = "Pick many",
        options: List<OptionConfig> = listOf(
            OptionConfig("Alpha"),
            OptionConfig("Beta")
        )
    ) = QuestionConfig(
        id = id, type = "CHECKBOX", text = text, isMandatory = false, options = options
    )

    // ========== Schedule Tests ==========

    @Test
    fun `build with MANUAL schedule returns Manual`() {
        val survey = SurveyBuilder.build(minimalConfig(scheduleType = "MANUAL"))
        assertTrue(survey.scheduleMethod is SurveyScheduleMethod.Manual)
    }

    @Test
    fun `build with TIME_OF_DAY schedule returns Fixed with parsed times`() {
        val survey = SurveyBuilder.build(
            minimalConfig(
                scheduleType = "TIME_OF_DAY",
                timeOfDay = listOf("09:30", "18:00")
            )
        )
        val method = survey.scheduleMethod
        assertTrue(method is SurveyScheduleMethod.Fixed)

        val fixed = method as SurveyScheduleMethod.Fixed
        assertEquals(2, fixed.timeOfDay.size)
        // 09:30 = 9*60 + 30 = 570 minutes = 34200000 ms
        assertEquals(
            TimeUnit.HOURS.toMillis(9) + TimeUnit.MINUTES.toMillis(30),
            fixed.timeOfDay[0]
        )
        // 18:00 = 64800000 ms
        assertEquals(TimeUnit.HOURS.toMillis(18), fixed.timeOfDay[1])
    }

    @Test
    fun `build with ESM schedule returns ESM with correct params`() {
        val esm = EsmConfig(
            numSurvey = 5,
            minInterval = 1800000,
            maxInterval = 7200000,
            startOfDay = 28800000,
            endOfDay = 72000000
        )
        val survey = SurveyBuilder.build(minimalConfig(scheduleType = "ESM", esm = esm))
        val method = survey.scheduleMethod
        assertTrue(method is SurveyScheduleMethod.ESM)

        val esmResult = method as SurveyScheduleMethod.ESM
        assertEquals(5, esmResult.numSurvey)
        assertEquals(1800000L, esmResult.minInterval)
        assertEquals(7200000L, esmResult.maxInterval)
    }

    @Test
    fun `build with ESM schedule but null config falls back to Manual`() {
        val survey = SurveyBuilder.build(minimalConfig(scheduleType = "ESM", esm = null))
        assertTrue(survey.scheduleMethod is SurveyScheduleMethod.Manual)
    }

    @Test
    fun `unknown schedule type falls back to Manual`() {
        val survey = SurveyBuilder.build(minimalConfig(scheduleType = "UNKNOWN_TYPE"))
        assertTrue(survey.scheduleMethod is SurveyScheduleMethod.Manual)
    }

    // ========== Notification Tests ==========

    @Test
    fun `notification config is correctly mapped`() {
        val survey = SurveyBuilder.build(minimalConfig())
        assertEquals("Test", survey.notificationConfig.title)
        assertEquals("A test survey", survey.notificationConfig.description)
    }

    // ========== Question Building Tests ==========

    @Test
    fun `TEXT question is built correctly`() {
        val survey = SurveyBuilder.build(minimalConfig(questions = listOf(textQuestion())))
        assertEquals(1, survey.flatQuestions.size)
        val q = survey.flatQuestions[0]
        assertTrue(q is TextQuestion)
        assertEquals(1, q.id)
        assertEquals("What?", q.question)
        assertTrue(q.isMandatory)
    }

    @Test
    fun `NUMBER question is built correctly`() {
        val survey = SurveyBuilder.build(minimalConfig(questions = listOf(numberQuestion())))
        assertEquals(1, survey.flatQuestions.size)
        val q = survey.flatQuestions[0]
        assertTrue(q is NumberQuestion)
        assertEquals(2, q.id)
        assertFalse(q.isMandatory)
    }

    @Test
    fun `RADIO question is built correctly with options`() {
        val survey = SurveyBuilder.build(minimalConfig(questions = listOf(radioQuestion())))
        assertEquals(1, survey.flatQuestions.size)
        val q = survey.flatQuestions[0] as SingleSelectionQuestion
        assertEquals(3, q.option.size)
        assertEquals("Option A", q.option[0].displayText)
        assertFalse(q.option[0].allowFreeResponse)
        assertTrue(q.option[2].allowFreeResponse)
    }

    @Test
    fun `CHECKBOX question is built correctly with options`() {
        val survey = SurveyBuilder.build(minimalConfig(questions = listOf(checkboxQuestion())))
        assertEquals(1, survey.flatQuestions.size)
        val q = survey.flatQuestions[0] as MultipleSelectionQuestion
        assertEquals(2, q.option.size)
        assertEquals("Alpha", q.option[0].displayText)
    }

    @Test
    fun `RADIO question with empty options returns null (skipped)`() {
        val q = radioQuestion(options = emptyList())
        val survey = SurveyBuilder.build(minimalConfig(questions = listOf(q)))
        assertEquals(0, survey.flatQuestions.size)
    }

    @Test
    fun `CHECKBOX question with empty options returns null (skipped)`() {
        val q = checkboxQuestion(options = emptyList())
        val survey = SurveyBuilder.build(minimalConfig(questions = listOf(q)))
        assertEquals(0, survey.flatQuestions.size)
    }

    @Test
    fun `unknown question type is skipped`() {
        val unknown = QuestionConfig(
            id = 99, type = "SLIDER", text = "Slide me", isMandatory = false
        )
        val survey = SurveyBuilder.build(minimalConfig(questions = listOf(unknown)))
        assertEquals(0, survey.flatQuestions.size)
    }

    @Test
    fun `multiple questions are built in order`() {
        val survey = SurveyBuilder.build(
            minimalConfig(
                questions = listOf(
                    textQuestion(id = 1),
                    numberQuestion(id = 2),
                    radioQuestion(id = 3),
                    checkboxQuestion(id = 4)
                )
            )
        )
        assertEquals(4, survey.flatQuestions.size)
        assertTrue(survey.flatQuestions[0] is TextQuestion)
        assertTrue(survey.flatQuestions[1] is NumberQuestion)
        assertTrue(survey.flatQuestions[2] is SingleSelectionQuestion)
        assertTrue(survey.flatQuestions[3] is MultipleSelectionQuestion)
    }

    // ========== Child / Trigger Tests ==========

    @Test
    fun `TEXT question with child trigger builds nested questions`() {
        val parentQ = QuestionConfig(
            id = 1, type = "TEXT", text = "Name?", isMandatory = true,
            children = listOf(
                ChildQuestionConfig(
                    trigger = TriggerConfig(op = "Equal", value = JsonPrimitive("test")),
                    questions = listOf(
                        QuestionConfig(
                            id = 2, type = "TEXT", text = "Follow-up?", isMandatory = false
                        )
                    )
                )
            )
        )
        val survey = SurveyBuilder.build(minimalConfig(questions = listOf(parentQ)))
        // flatQuestions includes both parent and child
        assertEquals(2, survey.flatQuestions.size)
        assertEquals(1, survey.flatQuestions[0].id)
        assertEquals(2, survey.flatQuestions[1].id)
    }

    @Test
    fun `question with null children builds without triggers`() {
        val q = textQuestion()
        val survey = SurveyBuilder.build(minimalConfig(questions = listOf(q)))
        assertEquals(1, survey.flatQuestions.size)
        assertTrue(survey.flatQuestions[0].children.isEmpty())
    }

    // ========== Time Parsing Edge Cases ==========

    @Test
    fun `TIME_OF_DAY with raw milliseconds string`() {
        val survey = SurveyBuilder.build(
            minimalConfig(
                scheduleType = "TIME_OF_DAY",
                timeOfDay = listOf("3600000") // 1 hour in ms
            )
        )
        val fixed = survey.scheduleMethod as SurveyScheduleMethod.Fixed
        assertEquals(3600000L, fixed.timeOfDay[0])
    }

    @Test
    fun `TIME_OF_DAY with midnight format`() {
        val survey = SurveyBuilder.build(
            minimalConfig(
                scheduleType = "TIME_OF_DAY",
                timeOfDay = listOf("0:00")
            )
        )
        val fixed = survey.scheduleMethod as SurveyScheduleMethod.Fixed
        assertEquals(0L, fixed.timeOfDay[0])
    }

    @Test
    fun `empty survey has no questions`() {
        val survey = SurveyBuilder.build(minimalConfig())
        assertTrue(survey.flatQuestions.isEmpty())
    }
}
