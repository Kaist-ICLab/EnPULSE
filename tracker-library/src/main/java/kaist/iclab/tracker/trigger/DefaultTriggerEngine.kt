package kaist.iclab.tracker.trigger

import kotlinx.serialization.json.JsonElement

class DefaultTriggerEngine : TriggerEngine {
    private val rules = mutableMapOf<String, TriggerRule>()
    private val actionHandlers = mutableMapOf<String, TriggerAction>()

    override fun addRule(rule: TriggerRule) {
        rules[rule.id] = rule
    }

    override fun removeRule(ruleId: String) {
        rules.remove(ruleId)
    }

    override fun registerActionHandler(actionType: String, handler: TriggerAction) {
        actionHandlers[actionType] = handler
    }

    override suspend fun processData(sensorId: String, timestamp: Long, data: JsonElement) {
        // Find rules that apply to this sensor
        val applicableRules = rules.values.filter { it.sourceSensorId == sensorId }
        if (applicableRules.isEmpty()) return

        // Basic implementation stub: In a real scenario, this would manage windowing buffers,
        // perform aggregations, check conditions, and then execute actions.
        
        // For demonstration of architecture, we just pass through
        // (The actual windowing and calculation logic goes here)
    }
}
