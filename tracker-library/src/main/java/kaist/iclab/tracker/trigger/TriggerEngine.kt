package kaist.iclab.tracker.trigger.engine

import kaist.iclab.tracker.trigger.model.ParsedCampaignTrigger
import kaist.iclab.tracker.trigger.model.TriggerEvaluationEvent
import kotlinx.coroutines.flow.SharedFlow

/**
 * Core trigger engine interface.
 *
 * The engine observes sensor detection state changes (via [DetectionStateTracker])
 * and evaluates condition trees defined in [ParsedCampaignTrigger] configurations.
 * When conditions are met and throttle checks pass, it delegates action execution
 * to a registered [TriggerActionHandler].
 *
 * ## Lifecycle
 * 1. [setActionHandler] — Register the app-specific action handler.
 * 2. [loadTriggers] — Load trigger configurations (e.g., received from phone via BLE).
 * 3. [start] — Begin observing detection state changes and evaluating triggers.
 * 4. [stop] — Stop evaluation (e.g., when data collection stops).
 *
 * ## Observability
 * The [evaluationEvents] flow emits a [TriggerEvaluationEvent] after every evaluation
 * cycle, providing full visibility into what the engine is doing — useful for debugging
 * and testing.
 */
interface TriggerEngine {

    /**
     * Load trigger configurations into the engine.
     * Replaces any previously loaded triggers. Clears throttle history.
     *
     * @param triggers The list of parsed triggers to evaluate.
     */
    fun loadTriggers(triggers: List<ParsedCampaignTrigger>)

    /**
     * Register the action handler that will execute triggered actions.
     * Must be called before [start].
     *
     * @param handler The app-specific action handler.
     */
    fun setActionHandler(handler: TriggerActionHandler)

    /**
     * Start the trigger evaluation loop.
     * The engine will observe [DetectionStateTracker.stateChanges] and evaluate
     * all loaded triggers whenever a sensor detection state changes.
     */
    fun start()

    /**
     * Stop the trigger evaluation loop.
     * Does not clear loaded triggers or the action handler.
     */
    fun stop()

    /**
     * Observable flow of evaluation events for debugging/testing.
     * Emits one [TriggerEvaluationEvent] per trigger per evaluation cycle.
     */
    val evaluationEvents: SharedFlow<TriggerEvaluationEvent>
}
