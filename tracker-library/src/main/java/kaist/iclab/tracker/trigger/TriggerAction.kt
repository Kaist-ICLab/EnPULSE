package kaist.iclab.tracker.trigger

/**
 * Represents an action to be executed when a trigger rule condition is met.
 */
interface TriggerAction {
    /**
     * Executes the action.
     * @param rule The rule that triggered the action.
     * @param calculatedValue The value that satisfied the condition.
     */
    suspend fun execute(rule: TriggerRule, calculatedValue: Double)
}
