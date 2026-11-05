package kaist.iclab.tracker.trigger.eval

import kaist.iclab.tracker.trigger.TriggerConstants
import kaist.iclab.tracker.trigger.model.ConditionNode
import kaist.iclab.tracker.trigger.model.DetectionState

/**
 * Evaluates a [ConditionNode] boolean expression tree against a map of current
 * sensor detection states.
 *
 * This is a pure, stateless function with no side effects — ideal for unit testing.
 *
 * Example:
 * ```kotlin
 * val condition = ConditionNode.And(listOf(
 *     ConditionNode.Detection("stress", "High"),
 *     ConditionNode.Not(ConditionNode.Detection("physical_activity", "In Vehicle"))
 * ))
 * val states = mapOf(
 *     "stress" to DetectionState("High", System.currentTimeMillis()),
 *     "physical_activity" to DetectionState("Drinking", System.currentTimeMillis())
 * )
 * val result = ConditionEvaluator.evaluate(condition, states) // true
 * ```
 */
object ConditionEvaluator {

    /**
     * Recursively evaluates the condition tree.
     *
     * @param node The root of the condition tree to evaluate.
     * @param states Current detection states keyed by sensor name.
     * @param now Current time in epoch milliseconds, used to discard stale states.
     *   Defaults to [System.currentTimeMillis]; tests can pass a fixed value.
     * @param maxAgeMillis A [ConditionNode.Detection] whose state is older than this is
     *   treated as if the sensor had never reported anything. [DetectionStateTracker]
     *   keeps the latest state forever, so without this a one-off event (e.g. a gesture)
     *   would satisfy its condition indefinitely, including for the next device user.
     * @param sensorMaxAgeMillis Per-sensor limits that replace [maxAgeMillis] for the sensors
     *   they name (see [TriggerConstants.Evaluation.SENSOR_MAX_AGE_MILLIS]).
     * @return `true` if the condition tree is satisfied, `false` otherwise.
     *
     * Note: If a [ConditionNode.Detection] references a sensor that has no entry
     * in [states] (i.e., the sensor hasn't produced any detection yet), the node
     * evaluates to `false`.
     */
    fun evaluate(
        node: ConditionNode,
        states: Map<String, DetectionState>,
        now: Long = System.currentTimeMillis(),
        maxAgeMillis: Long = TriggerConstants.Evaluation.MAX_DETECTION_AGE_MILLIS,
        sensorMaxAgeMillis: Map<String, Long> = TriggerConstants.Evaluation.SENSOR_MAX_AGE_MILLIS
    ): Boolean {
        return when (node) {
            is ConditionNode.And -> node.children.all { evaluate(it, states, now, maxAgeMillis, sensorMaxAgeMillis) }
            is ConditionNode.Or -> node.children.any { evaluate(it, states, now, maxAgeMillis, sensorMaxAgeMillis) }
            is ConditionNode.Not -> !evaluate(node.child, states, now, maxAgeMillis, sensorMaxAgeMillis)
            is ConditionNode.Detection -> {
                val state = states[node.sensor] ?: return false
                val limit = sensorMaxAgeMillis[node.sensor] ?: maxAgeMillis
                state.value == node.value && now - state.timestamp <= limit
            }
        }
    }
}
