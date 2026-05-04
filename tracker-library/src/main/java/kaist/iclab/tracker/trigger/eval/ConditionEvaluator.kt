package kaist.iclab.tracker.trigger.eval

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
     * @return `true` if the condition tree is satisfied, `false` otherwise.
     *
     * Note: If a [ConditionNode.Detection] references a sensor that has no entry
     * in [states] (i.e., the sensor hasn't produced any detection yet), the node
     * evaluates to `false`.
     */
    fun evaluate(node: ConditionNode, states: Map<String, DetectionState>): Boolean {
        return when (node) {
            is ConditionNode.And -> node.children.all { evaluate(it, states) }
            is ConditionNode.Or -> node.children.any { evaluate(it, states) }
            is ConditionNode.Not -> !evaluate(node.child, states)
            is ConditionNode.Detection -> states[node.sensor]?.value == node.value
        }
    }
}
