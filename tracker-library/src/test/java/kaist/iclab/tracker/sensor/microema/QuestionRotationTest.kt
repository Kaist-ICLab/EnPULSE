package kaist.iclab.tracker.sensor.microema

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuestionRotationTest {

    private fun question(id: Int) =
        WatchQuestion(id = id, surveyId = 1, text = "Q$id", answerType = AnswerType.values().first())

    private val questions = listOf(question(30), question(10), question(20))

    @Test
    fun `starts at the first question when none was asked`() {
        assertEquals(30, nextQuestionInRotation(questions, -1)?.id)
    }

    @Test
    fun `follows the configured order rather than id order`() {
        assertEquals(10, nextQuestionInRotation(questions, 30)?.id)
        assertEquals(20, nextQuestionInRotation(questions, 10)?.id)
    }

    @Test
    fun `wraps to the first question after the last`() {
        assertEquals(30, nextQuestionInRotation(questions, 20)?.id)
    }

    @Test
    fun `starts over when the last question was removed`() {
        assertEquals(30, nextQuestionInRotation(questions, 99)?.id)
    }

    @Test
    fun `a survey with no questions has no next question`() {
        assertNull(nextQuestionInRotation(emptyList(), -1))
    }
}
