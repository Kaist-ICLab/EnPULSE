package kaist.iclab.mobiletracker.services.upload.handlers

/**
 * Registry for all sensor upload handlers.
 * Provides lookup by sensor ID for upload operations.
 */
class SensorUploadHandlerRegistry(
    private val handlers: List<SensorUploadHandler>
) {
    private val handlerMap: Map<String, SensorUploadHandler> = handlers.associateBy { it.sensorId }

    /**
     * Get a handler for a specific sensor ID.
     * @param sensorId The sensor ID to look up
     * @return The handler, or null if not found
     */
    fun getHandler(sensorId: String): SensorUploadHandler? = handlerMap[sensorId]

    /**
     * Get all registered handlers.
     * @return List of all handlers
     */
    fun getAllHandlers(): List<SensorUploadHandler> = handlers
}
