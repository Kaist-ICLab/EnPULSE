package kaist.iclab.mobiletracker.webapp.bridge

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Request envelope posted by the TS bridge client (`@enpulse/webapp-bridge`, Phase 3) via
 * `EnPulseNative.postMessage`. [requestId] lets the TS side multiplex concurrent async calls.
 */
@Serializable
data class BridgeRequest(
    val requestId: String,
    val action: String,
    val payload: JsonElement
)

@Serializable
data class BridgeResponse(
    val requestId: String,
    val status: String, // "success" | "error"
    val data: JsonElement? = null,
    val errorMessage: String? = null
)
