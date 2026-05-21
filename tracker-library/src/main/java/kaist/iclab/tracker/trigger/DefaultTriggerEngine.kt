package kaist.iclab.tracker.trigger.engine

import android.content.Context
import android.os.PowerManager
import android.util.Log
import kaist.iclab.tracker.trigger.TriggerConstants
import kaist.iclab.tracker.trigger.eval.ConditionEvaluator
import kaist.iclab.tracker.trigger.model.ParsedCampaignTrigger
import kaist.iclab.tracker.trigger.model.TriggerActionConfig
import kaist.iclab.tracker.trigger.model.TriggerEvaluationEvent
import kaist.iclab.tracker.trigger.state.DetectionStateTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Default implementation of [TriggerEngine].
 *
 * Observes detection state changes from [DetectionStateTracker], evaluates
 * all loaded trigger condition trees using [ConditionEvaluator], applies
 * per-action throttling, and delegates action execution to a [TriggerActionHandler].
 *
 * ## Logging
 * Produces detailed log output under the tag "TriggerEngine" for debugging:
 * ```
 * D/TriggerEngine: State changed: stress → High
 * D/TriggerEngine: Evaluating trigger "Trigger X" (id=5)
 * D/TriggerEngine:   States: {stress=High, physical_activity=Drinking}
 * D/TriggerEngine:   Condition result: TRUE ✓
 * D/TriggerEngine:   Action[0] watch_ema(34): EXECUTING
 * D/TriggerEngine:   Action[1] ema(159): THROTTLED (30s < 500s min)
 * ```
 *
 * ## Observability
 * All evaluation results are also emitted on [evaluationEvents] for programmatic access.
 */
class DefaultTriggerEngine(
    private val context: Context,
    private val detectionStateTracker: DetectionStateTracker,
    private val coroutineScope: CoroutineScope
) : TriggerEngine {

    companion object {
        private const val TAG = "TriggerEngine"
    }

    private var triggers = listOf<ParsedCampaignTrigger>()
    private var handler: TriggerActionHandler? = null
    
    private val prefs by lazy { 
        context.getSharedPreferences(TriggerConstants.Throttling.PREFS_NAME, Context.MODE_PRIVATE) 
    }
    private val lastActionTimes = ConcurrentHashMap<String, Long>()

    init {
        loadThrottlingState()
    }

    private fun loadThrottlingState() {
        try {
            val allEntries = prefs.all
            for ((key, value) in allEntries) {
                if (value is Long) {
                    lastActionTimes[key] = value
                }
            }
            Log.d(TAG, "Restored ${lastActionTimes.size} throttling cooldowns from persistent storage")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load throttling state: ${e.message}")
        }
    }
    private var job: Job? = null
    private val powerManager by lazy { context.getSystemService(Context.POWER_SERVICE) as PowerManager }

    private val _evaluationEvents = MutableSharedFlow<TriggerEvaluationEvent>(
        extraBufferCapacity = 32
    )
    override val evaluationEvents: SharedFlow<TriggerEvaluationEvent> = _evaluationEvents

    override fun loadTriggers(triggers: List<ParsedCampaignTrigger>) {
        val currentIds = this.triggers.map { it.id }.toSet()
        val newIds = triggers.map { it.id }.toSet()
        
        if (currentIds.isNotEmpty() && currentIds != newIds) {
            lastActionTimes.clear()
            prefs.edit().clear().apply()
            Log.d(TAG, "Trigger set changed, cleared throttle state")
        }

        this.triggers = triggers
        Log.d(TAG, "Loaded ${triggers.size} trigger(s): ${triggers.map { it.name }}")
    }

    override fun setActionHandler(handler: TriggerActionHandler) {
        this.handler = handler
        Log.d(TAG, "Action handler registered: ${handler::class.simpleName}")
    }

    override fun start() {
        if (job != null) {
            Log.w(TAG, "Engine already running, ignoring start()")
            return
        }
        Log.d(TAG, "Starting trigger evaluation loop")
        job = coroutineScope.launch {
            detectionStateTracker.stateChanges.collect { (sensor, state) ->
                Log.d(TAG, "State changed: $sensor → ${state.value}")
                evaluateAllTriggers()
            }
        }
    }

    override fun stop() {
        job?.cancel()
        job = null
        Log.d(TAG, "Stopped trigger evaluation loop")
    }

    private suspend fun evaluateAllTriggers() {
        // Acquire a WakeLock to ensure the CPU doesn't sleep during evaluation
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            TriggerConstants.WakeLock.TAG
        )
        // Set a timeout to prevent battery drain if something gets stuck
        wakeLock.acquire(TriggerConstants.WakeLock.TIMEOUT_MS)

        try {
            val currentStates = detectionStateTracker.getAllStates()
            val h = handler

            if (h == null) {
                Log.w(TAG, "No action handler registered — skipping evaluation")
                return
            }

            for (trigger in triggers) {
                val result = ConditionEvaluator.evaluate(trigger.condition, currentStates)
                val executed = mutableListOf<String>()
                val throttled = mutableListOf<String>()

                Log.d(TAG, "Evaluating trigger \"${trigger.name}\" (id=${trigger.id})")
                Log.d(
                    TAG,
                    "  States: ${currentStates.map { "${it.key}=${it.value.value}" }}"
                )
                Log.d(TAG, "  Condition result: ${if (result) "TRUE ✓" else "FALSE ✗"}")

                if (result) {
                    val now = System.currentTimeMillis()
                    for ((index, action) in trigger.actions.withIndex()) {
                        val throttleKey = "${trigger.id}_$index"
                        val lastTime = lastActionTimes[throttleKey] ?: 0L
                        val elapsed = now - lastTime
                        val actionDesc = describeAction(action)

                        if (elapsed >= action.minIntervalMillis) {
                            Log.d(TAG, "  Action[$index] $actionDesc: EXECUTING")
                            try {
                                h.onAction(trigger, action)
                                lastActionTimes[throttleKey] = now
                                prefs.edit().putLong(throttleKey, now).apply()
                                executed.add(actionDesc)
                            } catch (e: Exception) {
                                Log.e(TAG, "  Action[$index] $actionDesc: ERROR - ${e.message}", e)
                            }
                        } else {
                            val elapsedSec = elapsed / 1000
                            val minSec = action.minIntervalMillis / 1000
                            Log.d(
                                TAG,
                                "  Action[$index] $actionDesc: THROTTLED (${elapsedSec}s < ${minSec}s min)"
                            )
                            throttled.add(actionDesc)
                        }
                    }
                }

                _evaluationEvents.tryEmit(
                    TriggerEvaluationEvent(
                        triggerId = trigger.id,
                        triggerName = trigger.name,
                        detectionStates = currentStates.mapValues { it.value.value },
                        conditionResult = result,
                        actionsExecuted = executed,
                        actionsThrottled = throttled,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        } finally {
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        }
    }

    private fun describeAction(action: TriggerActionConfig): String {
        return when (action) {
            is TriggerActionConfig.WatchEma -> "watch_ema(surveyId=${action.surveyId})"
            is TriggerActionConfig.Ema -> "ema(surveyId=${action.surveyId})"
            is TriggerActionConfig.Broadcast -> "broadcast(${action.action})"
        }
    }
}
