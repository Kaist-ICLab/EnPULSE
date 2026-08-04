package kaist.iclab.mobiletracker.webapp.bridge

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import kaist.iclab.mobiletracker.repository.WatchConnectionStatus
import kaist.iclab.mobiletracker.repository.WatchSensorRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class DeviceBridgeHandler(
    private val context: Context,
    private val watchSensorRepository: WatchSensorRepository
) {
    fun getDeviceInfo(request: BridgeRequest): BridgeResponse {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

        val data = buildJsonObject {
            put("batteryLevel", batteryLevel)
            put("manufacturer", Build.MANUFACTURER)
            put("model", Build.MODEL)
            put("osVersion", Build.VERSION.RELEASE)
            put("sdkInt", Build.VERSION.SDK_INT)
        }

        return BridgeResponse(request.requestId, "success", data = data)
    }

    suspend fun getWatchConnectionStatus(request: BridgeRequest): BridgeResponse {
        val info = withTimeoutOrNull(3000L) {
            watchSensorRepository.getWatchConnectionInfo().first()
        } ?: return BridgeResponse(request.requestId, "error", errorMessage = "Timeout querying watch status")

        val data = buildJsonObject {
            put("status", info.status.name)
            // JSON put requires a JsonElement, so we format the list to a string
            put("connectedDevices", info.connectedDevices.joinToString(", "))
        }

        return BridgeResponse(request.requestId, "success", data = data)
    }
}
