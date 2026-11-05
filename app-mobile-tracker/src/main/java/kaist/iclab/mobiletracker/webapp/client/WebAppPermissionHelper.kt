package kaist.iclab.mobiletracker.webapp.client

import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import kaist.iclab.mobiletracker.webapp.bridge.BridgeRequest
import kaist.iclab.mobiletracker.webapp.bridge.BridgeResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Encapsulates the runtime permission request queue and launcher logic for WebView to keep
 * WebAppActivity clean.
 */
class WebAppPermissionHelper(
    activity: ComponentActivity,
    private val onPermissionResponse: (b64Response: String) -> Unit
) {
    /** Queue of incoming permission requests to prevent dropping callbacks when requests overlap. */
    private val permissionRequestQueue = ConcurrentLinkedQueue<Pair<List<String>, BridgeRequest>>()

    /** Currently active permission request awaiting user response. */
    private var activePermissionRequest: BridgeRequest? = null

    private val requestPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val request = activePermissionRequest
        if (request != null) {
            val responseData = buildJsonObject {
                result.forEach { (perm, isGranted) -> put(perm, isGranted) }
            }
            val response = BridgeResponse(request.requestId, "success", data = responseData)
            val jsonStr = Json.encodeToString(BridgeResponse.serializer(), response)
            val b64 = android.util.Base64.encodeToString(
                jsonStr.toByteArray(),
                android.util.Base64.NO_WRAP
            )
            onPermissionResponse(b64)
            activePermissionRequest = null
            processNextPermissionRequest()
        }
    }

    /**
     * Enqueues a permission request and triggers it if no request is currently active.
     */
    fun requestPermissions(permissions: List<String>, request: BridgeRequest) {
        permissionRequestQueue.offer(Pair(permissions, request))
        processNextPermissionRequest()
    }

    private fun processNextPermissionRequest() {
        if (activePermissionRequest != null) return
        val next = permissionRequestQueue.poll() ?: return
        activePermissionRequest = next.second
        requestPermissionLauncher.launch(next.first.toTypedArray())
    }
}
