package kaist.iclab.mobiletracker.webapp.bridge

import android.util.Log
import kaist.iclab.mobiletracker.db.entity.phone.WebAppLogEntity
import kaist.iclab.mobiletracker.db.obx.WebAppLogStore
import kaist.iclab.mobiletracker.webapp.WebAppRegistry
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/**
 * Handles the `log` bridge action, letting a webapp record its own analytics events. The TS client
 * calls it as:
 *
 * ```ts
 * export function log(event_type: string, properties: string): Promise<void> {
 *     return callBridge("log", { action: "logEvent", event_type, properties });
 * }
 * ```
 *
 * so `properties` arrives as JSON *text*, and both `log` and `logEvent` are accepted as the action
 * name (see [EnPulseBridge]'s dispatch).
 *
 * Unlike the other bridge handlers there is no sensor behind this — the JS call itself is the
 * capture point. Events are written straight to [WebAppLogStore] and pushed to the `web_app_log`
 * table by [WebAppLogUploader] on its own periodic schedule (see
 * [kaist.iclab.mobiletracker.services.AutoSyncService.startWebAppLogSync]) — same
 * collect-locally-then-batch-upload shape as sensor data, deliberately bypassing the sensor
 * pipeline itself: no `SensorDescriptor`, no campaign `campaign_table` gating, and no dependency
 * on sensor collection being started.
 */
class LogBridgeHandler(
    private val webAppLogStore: WebAppLogStore,
    private val webAppRegistry: WebAppRegistry
) {
    fun log(request: BridgeRequest, callerWebAppId: String): BridgeResponse {
        if (webAppRegistry.get(callerWebAppId) == null) {
            return BridgeResponse(request.requestId, "error", errorMessage = "Unknown caller webapp: $callerWebAppId")
        }
        Log.d(TAG, "log: $request")

        val params = request.payload.jsonObject
        val eventType = (params["event_type"] as? JsonPrimitive)?.contentOrNullSafe()?.takeIf { it.isNotBlank() }
            ?: return BridgeResponse(request.requestId, "error", errorMessage = "Missing event_type")

        // Local write only — no immediate flush. AutoSyncService's periodic loop drains this store
        // into Supabase, same as sensor data: capture and upload are decoupled so a burst of log()
        // calls coalesces into a few upload batches instead of one network round-trip per event.
        webAppLogStore.insert(
            WebAppLogEntity(
                timestamp = System.currentTimeMillis(),
                webAppId = callerWebAppId,
                eventType = eventType,
                propertiesJson = params["properties"]?.let { normalizeProperties(it) }
            )
        )

        return BridgeResponse(request.requestId, "success")
    }

    /**
     * Coerces whatever the webapp passed as `properties` into valid JSON text for the `properties`
     * jsonb column. The documented contract is a JSON string, but an object/array is accepted too.
     * Text that isn't parseable JSON is stored as a JSON string literal rather than dropped —
     * [kaist.iclab.mobiletracker.db.obx.JsonStringElementSerializer] would otherwise silently
     * encode it as `null`.
     */
    private fun normalizeProperties(element: JsonElement): String? {
        val raw = when {
            element is JsonNull -> return null
            element is JsonPrimitive && element.isString -> element.content
            else -> return element.toString()
        }
        if (raw.isBlank()) return null
        return try {
            Json.parseToJsonElement(raw).toString()
        } catch (e: Exception) {
            JsonPrimitive(raw).toString()
        }
    }

    private fun JsonPrimitive.contentOrNullSafe(): String? = if (this is JsonNull) null else content

    companion object {
        private const val TAG = "LogBridgeHandler"
    }
}
