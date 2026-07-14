package kaist.iclab.tracker.trigger.engine

import kaist.iclab.tracker.trigger.model.ParsedCampaignTrigger
import kaist.iclab.tracker.trigger.model.TriggerActionConfig

/**
 * Callback interface for handling trigger actions.
 *
 * The trigger engine invokes this handler when a trigger's condition tree
 * evaluates to true and the action passes throttling checks.
 *
 * Implementations are **app-specific** — the library provides the evaluation
 * and throttling logic, while the app decides what to do with the result
 * (e.g., launch a MicroEMA survey, send a BLE message, fire a broadcast).
 *
 * Example implementation:
 * ```kotlin
 * class WatchTriggerActionHandler(...) : TriggerActionHandler {
 *     override suspend fun onAction(trigger: ParsedCampaignTrigger, action: TriggerActionConfig) {
 *         when (action) {
 *             is TriggerActionConfig.WatchEma -> launchMicroEma(action.surveyId)
 *             is TriggerActionConfig.Ema -> sendToPhone(action.surveyId)
 *             is TriggerActionConfig.Broadcast -> sendBroadcast(action)
 *         }
 *     }
 * }
 * ```
 */
interface TriggerActionHandler {

    /**
     * Called when a trigger's condition is met and the action passes throttle checks.
     *
     * @param trigger The trigger whose condition was satisfied.
     * @param action The specific action to execute from the trigger's action list.
     */
    suspend fun onAction(trigger: ParsedCampaignTrigger, action: TriggerActionConfig)
}
