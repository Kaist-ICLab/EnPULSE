package kaist.iclab.mobiletracker.webapp.bridge

import kaist.iclab.mobiletracker.db.obx.SupabaseJson
import kaist.iclab.mobiletracker.repository.PhoneSensorRepository
import kaist.iclab.mobiletracker.repository.Result
import kaist.iclab.mobiletracker.webapp.WebAppRegistry
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Handles the `logEvent` bridge action, letting a webapp record its own generic events
 * (`event_name` + arbitrary `properties`) so they're stored locally and uploaded to Supabase
 * alongside sensor data. Unlike a real sensor, there's no tracker-library `Sensor`/`Listener` behind
 * this — the JS call itself is the capture point, so this handler builds the [WebAppLogRecord]
 * directly and writes it through the same [PhoneSensorRepository.insertSensorData] path every
 * sensor's data-service loop uses (see the `WebAppLog` entry in
 * [kaist.iclab.mobiletracker.di.SensorRegistry] for the local-storage/upload wiring).
 */
class LogBridgeHandler(
    private val phoneSensorRepository: PhoneSensorRepository,
    private val webAppRegistry: WebAppRegistry
) {
    suspend fun logEvent(request: BridgeRequest, callerWebAppId: String): BridgeResponse {
        if (webAppRegistry.get(callerWebAppId) == null) {
            return BridgeResponse(request.requestId, "error", errorMessage = "Unknown caller webapp: $callerWebAppId")
        }

        val params = request.payload.jsonObject
        val eventName = params["event_name"]?.jsonPrimitive?.content
            ?: return BridgeResponse(request.requestId, "error", errorMessage = "Missing event_name")
        val timestamp = params["timestamp"]?.jsonPrimitive?.longOrNull ?: System.currentTimeMillis()
        val propertiesJson = params["properties"]?.let { SupabaseJson.encodeToString(JsonElement.serializer(), it) }

        val result = phoneSensorRepository.insertSensorData(
            SENSOR_ID,
            WebAppLogRecord(
                timestamp = timestamp,
                webAppId = callerWebAppId,
                eventName = eventName,
                propertiesJson = propertiesJson
            )
        )

        return when (result) {
            is Result.Success -> BridgeResponse(request.requestId, "success")
            is Result.Error -> BridgeResponse(request.requestId, "error", errorMessage = result.message)
        }
    }

    companion object {
        const val SENSOR_ID = "WebAppLog"
    }
}
