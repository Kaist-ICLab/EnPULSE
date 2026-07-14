package kaist.iclab.tracker.trigger.model

/**
 * Represents a single evaluation cycle of a trigger's condition tree.
 *
 * Emitted by the trigger engine after every evaluation for observability/debugging.
 * Consumers can collect these events to:
 * - Log evaluation results via `adb logcat`
 * - Display a debug panel in the app UI
 * - Record evaluation history for analysis
 *
 * @param triggerId The ID of the trigger being evaluated.
 * @param triggerName The human-readable name of the trigger.
 * @param detectionStates Snapshot of all sensor detection labels at evaluation time.
 * @param conditionResult Whether the condition tree evaluated to true.
 * @param actionsExecuted Names/descriptions of actions that were executed.
 * @param actionsThrottled Names/descriptions of actions that were skipped due to throttling.
 * @param timestamp Epoch milliseconds when the evaluation occurred.
 */
data class TriggerEvaluationEvent(
    val triggerId: Int,
    val triggerName: String,
    val detectionStates: Map<String, String>,
    val conditionResult: Boolean,
    val actionsExecuted: List<String>,
    val actionsThrottled: List<String>,
    val timestamp: Long
)
