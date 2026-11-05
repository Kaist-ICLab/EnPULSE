package kaist.iclab.tracker.sensor.survey.config

import kaist.iclab.tracker.sensor.survey.question.BinaryQuestion
import kaist.iclab.tracker.sensor.survey.question.Expression
import kaist.iclab.tracker.sensor.survey.question.NumberScaleQuestion
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Follow-up rules and labels as the dashboard writes them (see ANDROID-CORE-FLOW-REVIEW C-3, C-4, C-6). */
class SurveyBuilderFollowUpTest {

    private val scale = NumberScaleQuestion(
        id = 1, question = "How stressed?", isMandatory = false,
        min = 1, max = 10, minLabel = "", maxLabel = ""
    )
    private val yesNo = BinaryQuestion(id = 2, question = "Are you tired?", isMandatory = false)

    @Suppress("UNCHECKED_CAST")
    private fun scaleRule(op: String, value: Int) =
        SurveyBuilder.parseExpression(op, JsonPrimitive(value), "NUMBERSCALE") as Expression<Int?>

    @Suppress("UNCHECKED_CAST")
    private fun yesNoRule(op: String, optionIndex: Int) =
        SurveyBuilder.parseExpression(op, JsonPrimitive(optionIndex), "BINARY") as Expression<Boolean?>?

    // --- C-3: number-scale rules are whole numbers, matching NumberScaleQuestion's answers ---

    @Test
    fun `number-scale comparison rules evaluate instead of crashing`() {
        assertTrue(scale.eval(scaleRule("GreaterThanOrEqual", 7), 8))
        assertFalse(scale.eval(scaleRule("GreaterThan", 7), 7))
        assertTrue(scale.eval(scaleRule("LessThanOrEqual", 3), 3))
        assertFalse(scale.eval(scaleRule("LessThan", 3), 5))
    }

    @Test
    fun `number-scale equality rule matches the same answer`() {
        assertTrue(scale.eval(scaleRule("Equal", 5), 5))
        assertFalse(scale.eval(scaleRule("NotEqual", 5), 5))
    }

    // --- C-4: Yes/No rules store the option index (0 = Yes, 1 = No); answers are Yes = true ---

    @Test
    fun `yes-no rule on Yes shows the follow-up only after Yes`() {
        val ifYes = yesNoRule("Equal", 0)!!
        assertTrue(yesNo.eval(ifYes, true))
        assertFalse(yesNo.eval(ifYes, false))
    }

    @Test
    fun `yes-no rule on not-Yes shows the follow-up only after No`() {
        val ifNotYes = yesNoRule("NotEqual", 0)!!
        assertFalse(yesNo.eval(ifNotYes, true))
        assertTrue(yesNo.eval(ifNotYes, false))
    }

    @Test
    fun `yes-no rule on No shows the follow-up only after No`() {
        val ifNo = yesNoRule("Equal", 1)!!
        assertTrue(yesNo.eval(ifNo, false))
        assertFalse(yesNo.eval(ifNo, true))
    }

    @Test
    fun `yes-no rule with an unknown option index is ignored`() {
        assertNull(yesNoRule("Equal", 2))
    }

    // --- C-6 and C-3 end to end through SurveyBuilder.build ---

    private fun survey(vararg questions: QuestionConfig) = SurveyConfig(
        id = 10, campaignId = 1, title = "Check-in", description = null,
        scheduleType = ScheduleType.MANUAL, schedule = null, questions = questions.toList()
    )

    @Test
    fun `number-scale question with blank labels is kept`() {
        val built = SurveyBuilder.build(
            survey(QuestionConfig(id = 1, type = "NUMBERSCALE", text = "Mood", isMandatory = true, min = 1, max = 10))
        )
        assertEquals(1, built.flatQuestions.size)
        assertTrue(built.flatQuestions.single() is NumberScaleQuestion)
    }

    @Test
    fun `number-scale follow-up question is built with its parent`() {
        val built = SurveyBuilder.build(
            survey(
                QuestionConfig(id = 1, type = "NUMBERSCALE", text = "Mood", isMandatory = true, min = 1, max = 10),
                QuestionConfig(
                    id = 2, parentId = 1, type = "TEXT", text = "Why?", isMandatory = false,
                    trigger = """{"op":"GreaterThanOrEqual","value":7}"""
                )
            )
        )
        assertEquals(listOf(1, 2), built.flatQuestions.map { it.id })
        assertNotNull(built.flatQuestions.firstOrNull { it.id == 2 })
    }
}
