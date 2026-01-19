package kaist.iclab.mobiletracker.repository.handlers

/**
 * Registry for all sensor data handlers.
 * Provides lookup by sensor ID and iteration over all handlers.
 */
class SensorDataHandlerRegistry(
    private val handlers: List<SensorDataHandler>
) {
    private val handlerMap: Map<String, SensorDataHandler> = handlers.associateBy { it.sensorId }

    /**
     * Get a handler for a specific sensor ID.
     * @param sensorId The sensor ID to look up
     * @return The handler, or null if not found
     */
    fun getHandler(sensorId: String): SensorDataHandler? = handlerMap[sensorId]

    /**
     * Get all registered handlers.
     * @return List of all handlers
     */
    fun getAllHandlers(): List<SensorDataHandler> = handlers
}
