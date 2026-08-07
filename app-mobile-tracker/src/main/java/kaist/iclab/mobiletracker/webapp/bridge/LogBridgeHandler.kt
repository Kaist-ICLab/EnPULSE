package kaist.iclab.mobiletracker.webapp.bridge

import android.util.Log
import kaist.iclab.mobiletracker.db.entity.phone.WebAppLogEntity
import kaist.iclab.mobiletracker.db.obx.WebAppLogStore
import kaist.iclab.mobiletracker.di.AppCoroutineScope
import kaist.iclab.mobiletracker.services.upload.WebAppLogUploader
import kaist.iclab.mobiletracker.webapp.WebAppRegistry
import kotlinx.coroutines.launch
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
 * table by [WebAppLogUploader], deliberately bypassing the sensor pipeline: no
 * `SensorDescriptor`, no campaign `campaign_table` gating, and no dependency on sensor collection
 * being started.
 */
class LogBridgeHandler(
    private val webAppLogStore: WebAppLogStore,
    private val webAppLogUploader: WebAppLogUploader,
    private val webAppRegistry: WebAppRegistry,
    private val appScope: AppCoroutineScope
) {
    fun log(request: BridgeRequest, callerWebAppId: String): BridgeResponse {
        if (webAppRegistry.get(callerWebAppId) == null) {
            return BridgeResponse(request.requestId, "error", errorMessage = "Unknown caller webapp: $callerWebAppId")
        }

        val params = request.payload.jsonObject
        val eventType = (params["event_type"] as? JsonPrimitive)?.contentOrNullSafe()?.takeIf { it.isNotBlank() }
            ?: return BridgeResponse(request.requestId, "error", errorMessage = "Missing event_type")

        webAppLogStore.insert(
            WebAppLogEntity(
                timestamp = System.currentTimeMillis(),
                webAppId = callerWebAppId,
                eventType = eventType,
                propertiesJson = params["properties"]?.let { normalizeProperties(it) }
            )
        )

        // Fire-and-forget so the webapp's promise resolves on the local write, not the network.
        // The uploader drains everything pending under one lock, so a burst of log() calls
        // coalesces into a few inserts rather than one per event.
        appScope.io.launch {
            runCatching { webAppLogUploader.flush() }
                .onFailure { Log.w(TAG, "Immediate webapp log flush failed: ${it.message}") }
        }

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
