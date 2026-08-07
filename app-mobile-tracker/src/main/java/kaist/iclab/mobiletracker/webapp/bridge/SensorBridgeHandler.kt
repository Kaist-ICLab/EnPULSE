package kaist.iclab.mobiletracker.webapp.bridge

import kaist.iclab.mobiletracker.repository.WatchConnectionStatus
import kaist.iclab.mobiletracker.repository.WatchSensorRepository
import kaist.iclab.mobiletracker.repository.handlers.SensorDataHandlerRegistry
import kaist.iclab.mobiletracker.webapp.WebAppRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/**
 * Handles the `getSensorData` bridge action.
 *
 * Returns the raw serialized sensor entities (the same `@Serializable` shape used for Supabase
 * upload) from local storage, via [getRecordsJsonPaginated]. Per design principle #6, this does not
 * drive any new watch sync — the watch already pushes sensor data over BLE in the background; this
 * only reports whether that background pipeline currently looks connected, as a `sync_status` hint
 * for the webapp.
 */
class SensorBridgeHandler(
    private val handlerRegistry: SensorDataHandlerRegistry,
    private val watchSensorRepository: WatchSensorRepository,
    private val webAppRegistry: WebAppRegistry
) {
    suspend fun getSensorData(request: BridgeRequest, callerWebAppId: String): BridgeResponse {
        val params = request.payload.jsonObject
        val sensorId = params["sensor_id"]?.jsonPrimitive?.content
            ?: return BridgeResponse(request.requestId, "error", errorMessage = "Missing sensor_id")
        val startTime = params["start_time"]?.jsonPrimitive?.longOrNull
            ?: return BridgeResponse(request.requestId, "error", errorMessage = "Missing start_time")
        val endTime = params["end_time"]?.jsonPrimitive?.longOrNull
            ?: return BridgeResponse(request.requestId, "error", errorMessage = "Missing end_time")

        val webApp = webAppRegistry.get(callerWebAppId)
        if (webApp == null) {
            return BridgeResponse(request.requestId, "error", errorMessage = "Unknown caller webapp: $callerWebAppId")
        }

        val handler = handlerRegistry.getHandler(sensorId)
            ?: return BridgeResponse(request.requestId, "error", errorMessage = "Unknown sensor_id: $sensorId")

        val records = handler.getRecordsJsonPaginated(
            afterTimestamp = startTime,
            beforeTimestamp = endTime,
            isAscending = true,
            limit = MAX_RECORDS,
            offset = 0
        )

        val syncStatus = if (handler.isWatchSensor) resolveWatchSyncStatus() else "fresh"

        return BridgeResponse(
            request.requestId, "success",
            data = buildJsonObject {
                put("records", JsonArray(records))
                put("sync_status", syncStatus)
            }
        )
    }

    suspend fun getLatestSensorReading(request: BridgeRequest, callerWebAppId: String): BridgeResponse {
        val params = request.payload.jsonObject
        val sensorId = params["sensor_id"]?.jsonPrimitive?.content
            ?: return BridgeResponse(request.requestId, "error", errorMessage = "Missing sensor_id")

        val webApp = webAppRegistry.get(callerWebAppId)
        if (webApp == null) {
            return BridgeResponse(request.requestId, "error", errorMessage = "Unknown caller webapp: $callerWebAppId")
        }

        val handler = handlerRegistry.getHandler(sensorId)
            ?: return BridgeResponse(request.requestId, "error", errorMessage = "Unknown sensor_id: $sensorId")

        // Fetch just the single latest record
        val records = handler.getRecordsJsonPaginated(
            afterTimestamp = 0,
            beforeTimestamp = Long.MAX_VALUE,
            isAscending = false,
            limit = 1,
            offset = 0
        )

        val syncStatus = if (handler.isWatchSensor) resolveWatchSyncStatus() else "fresh"

        return BridgeResponse(
            request.requestId, "success",
            data = buildJsonObject {
                put("records", JsonArray(records))
                put("sync_status", syncStatus)
            }
        )
    }


    /** "fresh" if the watch currently looks reachable, "stale_fallback" otherwise, "timeout" if the
     *  connection check itself doesn't resolve in time. */
    private suspend fun resolveWatchSyncStatus(): String {
        val info = withTimeoutOrNull(SYNC_STATUS_TIMEOUT_MS) {
            watchSensorRepository.getWatchConnectionInfo().first()
        } ?: return "timeout"

        return if (info.status == WatchConnectionStatus.CONNECTED) "fresh" else "stale_fallback"
    }

    companion object {
        private const val MAX_RECORDS = 5000
        private const val SYNC_STATUS_TIMEOUT_MS = 3000L
    }
}
