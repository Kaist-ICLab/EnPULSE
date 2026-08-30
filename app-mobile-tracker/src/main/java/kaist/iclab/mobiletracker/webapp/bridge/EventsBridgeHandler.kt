package kaist.iclab.mobiletracker.webapp.bridge

import kaist.iclab.mobiletracker.repository.handlers.SensorDataHandlerRegistry
import kaist.iclab.tracker.sensor.common.ActivityRecognitionSensor
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

class EventsBridgeHandler(
    private val handlerRegistry: SensorDataHandlerRegistry
) {
    suspend fun getDailyActivities(request: BridgeRequest, callerWebAppId: String): BridgeResponse {
        val params = request.payload.jsonObject
        val startTime = params["start_time"]?.jsonPrimitive?.longOrNull ?: 0L
        val endTime = params["end_time"]?.jsonPrimitive?.longOrNull ?: Long.MAX_VALUE

        val handler = handlerRegistry.getHandler("ActivityRecognition")
            ?: return BridgeResponse(
                request.requestId,
                "error",
                errorMessage = "Activity sensor not found"
            )

        val records = handler.getRecordsJsonPaginated(startTime, endTime, true, 5000, 0)

        val mapped = buildJsonArray {
            records.forEach { record ->
                val obj = record.jsonObject
                add(buildJsonObject {
                    put("id", obj["eventId"]?.jsonPrimitive?.content ?: "")
                    put("timestamp", obj["timestamp"]?.jsonPrimitive?.longOrNull ?: 0L)
                    put(
                        "activityType",
                        ActivityRecognitionSensor.getActivityName(obj["activity_type"]?.jsonPrimitive?.intOrNull)
                    )
                    put("durationMs", 0L) // Default or computed later
                })
            }
        }

        return BridgeResponse(request.requestId, "success", data = mapped)
    }

    suspend fun getDailyLocations(request: BridgeRequest, callerWebAppId: String): BridgeResponse {
        val params = request.payload.jsonObject
        val startTime = params["start_time"]?.jsonPrimitive?.longOrNull ?: 0L
        val endTime = params["end_time"]?.jsonPrimitive?.longOrNull ?: Long.MAX_VALUE

        val handler = handlerRegistry.getHandler("Location")
            ?: return BridgeResponse(
                request.requestId,
                "error",
                errorMessage = "Location sensor not found"
            )

        val records = handler.getRecordsJsonPaginated(startTime, endTime, true, 5000, 0)

        val mapped = buildJsonArray {
            records.forEach { record ->
                val obj = record.jsonObject
                add(buildJsonObject {
                    put("id", obj["eventId"]?.jsonPrimitive?.content ?: "")
                    put("timestamp", obj["timestamp"]?.jsonPrimitive?.longOrNull ?: 0L)
                    put("category", "Unknown") // Requires reverse geocoding for real category
                    put("latitude", obj["latitude"]?.jsonPrimitive?.doubleOrNull ?: 0.0)
                    put("longitude", obj["longitude"]?.jsonPrimitive?.doubleOrNull ?: 0.0)
                })
            }
        }

        return BridgeResponse(request.requestId, "success", data = mapped)
    }

    suspend fun getDailyPhoneUsages(
        request: BridgeRequest,
        callerWebAppId: String
    ): BridgeResponse {
        val params = request.payload.jsonObject
        val startTime = params["start_time"]?.jsonPrimitive?.longOrNull ?: 0L
        val endTime = params["end_time"]?.jsonPrimitive?.longOrNull ?: Long.MAX_VALUE

        val handler = handlerRegistry.getHandler("AppUsage")
            ?: return BridgeResponse(
                request.requestId,
                "error",
                errorMessage = "AppUsage sensor not found"
            )

        val records = handler.getRecordsJsonPaginated(startTime, endTime, true, 5000, 0)

        val mapped = buildJsonArray {
            records.forEach { record ->
                val obj = record.jsonObject
                add(buildJsonObject {
                    put("id", obj["eventId"]?.jsonPrimitive?.content ?: "")
                    put("timestamp", obj["timestamp"]?.jsonPrimitive?.longOrNull ?: 0L)
                    put("packageName", obj["package_name"]?.jsonPrimitive?.content ?: "")
                    put("durationMs", 0L) // Event-based, duration computed later
                })
            }
        }

        return BridgeResponse(request.requestId, "success", data = mapped)
    }
}
