package kaist.iclab.mobiletracker.webapp.bridge

import kaist.iclab.mobiletracker.repository.SensorRecord
import kaist.iclab.mobiletracker.repository.WatchConnectionStatus
import kaist.iclab.mobiletracker.repository.WatchSensorRepository
import kaist.iclab.mobiletracker.repository.handlers.SensorDataHandlerRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/**
 * Handles the `getSensorData` bridge action.
 *
 * Only exposes the same derived [SensorRecord] projection the Data screen already shows (design
 * principle #5 — no raw sensor table access), read from local storage. Per design principle #6,
 * this does not drive any new watch sync — the watch already pushes sensor data over BLE in the
 * background; this only reports whether that background pipeline currently looks connected, as a
 * `sync_status` hint for the webapp.
 */
class SensorBridgeHandler(
    private val handlerRegistry: SensorDataHandlerRegistry,
    private val watchSensorRepository: WatchSensorRepository
) {
    suspend fun getSensorData(request: BridgeRequest): BridgeResponse {
        val params = request.payload.jsonObject
        val sensorId = params["sensor_id"]?.jsonPrimitive?.content
            ?: return BridgeResponse(request.requestId, "error", errorMessage = "Missing sensor_id")
        val startTime = params["start_time"]?.jsonPrimitive?.longOrNull
            ?: return BridgeResponse(request.requestId, "error", errorMessage = "Missing start_time")
        val endTime = params["end_time"]?.jsonPrimitive?.longOrNull
            ?: return BridgeResponse(request.requestId, "error", errorMessage = "Missing end_time")

        val handler = handlerRegistry.getHandler(sensorId)
            ?: return BridgeResponse(request.requestId, "error", errorMessage = "Unknown sensor_id: $sensorId")

        val records = handler.getRecordsPaginated(
            afterTimestamp = startTime,
            isAscending = true,
            limit = MAX_RECORDS,
            offset = 0
        ).filter { it.timestamp <= endTime }

        val syncStatus = if (handler.isWatchSensor) resolveWatchSyncStatus() else "fresh"

        return BridgeResponse(
            request.requestId, "success",
            data = buildJsonObject {
                put("records", Json.encodeToJsonElement(ListSerializer(SensorRecord.serializer()), records))
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
