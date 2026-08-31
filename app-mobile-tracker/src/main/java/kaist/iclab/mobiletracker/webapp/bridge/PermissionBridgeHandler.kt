package kaist.iclab.mobiletracker.webapp.bridge

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import kaist.iclab.tracker.permission.AndroidPermissionManager
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class PermissionBridgeHandler(
    private val context: Context,
    private val permissionManager: AndroidPermissionManager,
    private val requestPermissionAction: (List<String>, BridgeRequest) -> Unit
) {
    fun checkPermissions(request: BridgeRequest): BridgeResponse {
        val params = request.payload.jsonObject
        val permissionsArray = params["permissions"]?.jsonArray
        
        val data = buildJsonObject {
            permissionsArray?.forEach { element ->
                val perm = element.jsonPrimitive.content
                val isGranted = ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
                put(perm, isGranted)
            }
        }
        return BridgeResponse(request.requestId, "success", data = data)
    }

    fun requestPermissions(request: BridgeRequest): BridgeResponse? {
        val params = request.payload.jsonObject
        val permissionsArray = params["permissions"]?.jsonArray
        
        val permissionsToRequest = permissionsArray?.map { it.jsonPrimitive.content } ?: emptyList()
        
        if (permissionsToRequest.isEmpty()) {
            return BridgeResponse(request.requestId, "error", errorMessage = "No permissions requested")
        }

        // Delegate the actual UI request to the Activity. 
        // The Activity must send the response back asynchronously using the requestId.
        requestPermissionAction(permissionsToRequest, request)
        
        // Return null because the response is handled asynchronously.
        return null
    }
}
