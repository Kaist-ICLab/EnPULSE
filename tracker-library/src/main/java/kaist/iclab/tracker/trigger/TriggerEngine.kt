package kaist.iclab.tracker.trigger

import kotlinx.serialization.json.JsonElement

/**
 * The core engine that processes incoming sensor data against defined trigger rules.
 */
interface TriggerEngine {
    /**
     * Register a new rule with the engine.
     */
    fun addRule(rule: TriggerRule)

    /**
     * Remove an existing rule by ID.
     */
    fun removeRule(ruleId: String)

    /**
     * Register an action handler for a specific action type (e.g., "MICRO_EMA").
     */
    fun registerActionHandler(actionType: String, handler: TriggerAction)

    /**
     * Process incoming data for a specific sensor.
     *
     * @param sensorId The identifier of the sensor producing the data.
     * @param timestamp The time the data was recorded.
     * @param data The actual sensor data payload.
     */
    suspend fun processData(sensorId: String, timestamp: Long, data: JsonElement)
}
