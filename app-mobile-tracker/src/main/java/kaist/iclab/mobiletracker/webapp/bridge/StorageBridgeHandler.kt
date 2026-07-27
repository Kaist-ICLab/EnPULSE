package kaist.iclab.mobiletracker.webapp.bridge

import kaist.iclab.mobiletracker.webapp.storage.CouchbaseWebAppStorage
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Handles `getStorageData` / `setStorageData`. [callerWebAppId] (the webapp that owns the WebView
 * this bridge is attached to, not anything the webapp itself can claim) namespaces every key so
 * webapp A can never read or overwrite webapp B's storage.
 */
class StorageBridgeHandler(
    private val webAppStorageStore: CouchbaseWebAppStorage
) {
    fun get(request: BridgeRequest, callerWebAppId: String): BridgeResponse {
        val key = request.payload.jsonObject["key"]?.jsonPrimitive?.content
            ?: return BridgeResponse(request.requestId, "error", errorMessage = "Missing key")

        val value = webAppStorageStore.get(callerWebAppId, key) ?: JsonNull
        return BridgeResponse(request.requestId, "success", data = value)
    }

    fun set(request: BridgeRequest, callerWebAppId: String): BridgeResponse {
        val payload = request.payload.jsonObject
        val key = payload["key"]?.jsonPrimitive?.content
            ?: return BridgeResponse(request.requestId, "error", errorMessage = "Missing key")
        val value = payload["value"] ?: JsonNull

        webAppStorageStore.set(callerWebAppId, key, value)
        return BridgeResponse(request.requestId, "success")
    }
}
