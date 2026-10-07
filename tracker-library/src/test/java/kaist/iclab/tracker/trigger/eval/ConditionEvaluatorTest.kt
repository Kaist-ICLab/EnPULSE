package kaist.iclab.tracker.trigger.eval

import kaist.iclab.tracker.trigger.model.ConditionNode
import kaist.iclab.tracker.trigger.model.DetectionState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConditionEvaluatorTest {

    private val gestureClapping = ConditionNode.Detection("gesture", "Clapping")

    @Test
    fun `matching detection within the max age is true`() {
        val now = 1_000_000L
        val states = mapOf("gesture" to DetectionState("Clapping", now - 1_000))

        assertTrue(ConditionEvaluator.evaluate(gestureClapping, states, now, maxAgeMillis = 60_000))
    }

    @Test
    fun `matching detection older than the max age is false`() {
        val now = 1_000_000L
        val states = mapOf("gesture" to DetectionState("Clapping", now - 61_000))

        assertFalse(ConditionEvaluator.evaluate(gestureClapping, states, now, maxAgeMillis = 60_000))
    }

    @Test
    fun `detection exactly at the max age boundary is true`() {
        val now = 1_000_000L
        val states = mapOf("gesture" to DetectionState("Clapping", now - 60_000))

        assertTrue(ConditionEvaluator.evaluate(gestureClapping, states, now, maxAgeMillis = 60_000))
    }

    @Test
    fun `missing sensor is false regardless of age`() {
        val now = 1_000_000L
        assertFalse(ConditionEvaluator.evaluate(gestureClapping, emptyMap(), now, maxAgeMillis = 60_000))
    }

    @Test
    fun `wrong value is false even when fresh`() {
        val now = 1_000_000L
        val states = mapOf("gesture" to DetectionState("Waving", now - 1_000))

        assertFalse(ConditionEvaluator.evaluate(gestureClapping, states, now, maxAgeMillis = 60_000))
    }

    @Test
    fun `stale state inside a NOT still counts as absent, so NOT is true`() {
        val now = 1_000_000L
        val states = mapOf("gesture" to DetectionState("Clapping", now - 61_000))

        assertTrue(
            ConditionEvaluator.evaluate(
                ConditionNode.Not(gestureClapping), states, now, maxAgeMillis = 60_000
            )
        )
    }

    @Test
    fun `AND requires every child to be fresh`() {
        val now = 1_000_000L
        val states = mapOf(
            "gesture" to DetectionState("Clapping", now - 1_000),
            "stress" to DetectionState("High", now - 61_000)
        )
        val condition = ConditionNode.And(
            listOf(gestureClapping, ConditionNode.Detection("stress", "High"))
        )

        assertFalse(ConditionEvaluator.evaluate(condition, states, now, maxAgeMillis = 60_000))
    }

    @Test
    fun `a continuously reported sensor stays fresh across its normal update interval`() {
        val now = 1_000_000L
        // Stress reports every 30s; 29s since the last update is well inside the 60s window.
        val states = mapOf("stress" to DetectionState("High", now - 29_000))

        assertTrue(
            ConditionEvaluator.evaluate(
                ConditionNode.Detection("stress", "High"), states, now, maxAgeMillis = 60_000
            )
        )
    }

    @Test
    fun `default max age is 60 seconds`() {
        val now = System.currentTimeMillis()
        val justStale = mapOf("gesture" to DetectionState("Clapping", now - 60_001))
        val justFresh = mapOf("gesture" to DetectionState("Clapping", now - 59_999))

        assertFalse(ConditionEvaluator.evaluate(gestureClapping, justStale, now))
        assertTrue(ConditionEvaluator.evaluate(gestureClapping, justFresh, now))
    }

    @Test
    fun `activity stays valid for 30 minutes since it is not re-reported while still`() {
        val now = System.currentTimeMillis()
        val still = ConditionNode.Detection("physical_activity", "Still")
        val tenMinutesOld = mapOf("physical_activity" to DetectionState("Still", now - 10 * 60_000))
        val justStale = mapOf("physical_activity" to DetectionState("Still", now - 30 * 60_000 - 1))

        assertTrue(ConditionEvaluator.evaluate(still, tenMinutesOld, now))
        assertFalse(ConditionEvaluator.evaluate(still, justStale, now))
    }

    @Test
    fun `the longer activity limit does not apply to other sensors in the same condition`() {
        val now = System.currentTimeMillis()
        val condition = ConditionNode.And(
            listOf(gestureClapping, ConditionNode.Detection("physical_activity", "Still"))
        )
        val states = mapOf(
            "gesture" to DetectionState("Clapping", now - 5 * 60_000),
            "physical_activity" to DetectionState("Still", now - 5 * 60_000)
        )
        val freshGesture = states + ("gesture" to DetectionState("Clapping", now - 1_000))

        assertFalse(ConditionEvaluator.evaluate(condition, states, now))
        assertTrue(ConditionEvaluator.evaluate(condition, freshGesture, now))
    }
}
