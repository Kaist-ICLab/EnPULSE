package kaist.iclab.mobiletracker.webapp.bridge

import android.net.Uri
import android.util.Log
import android.webkit.WebView
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewCompat
import kaist.iclab.mobiletracker.di.AppCoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * `WebMessageListener` registered under the `EnPulseNative` JS object name. Only origins passed
 * to `WebViewCompat.addWebMessageListener` (the webapp's own [WebAppConfig.allowedOrigin]) can
 * reach this — that's the trust boundary, not anything checked here.
 *
 * API surface is intentionally narrow (design principle #5): no raw sensor tables, only the
 * derived actions below.
 */
class EnPulseBridge(
    private val surveyHandler: SurveyBridgeHandler,
    private val sensorHandler: SensorBridgeHandler,
    private val storageHandler: StorageBridgeHandler,
    private val deviceHandler: DeviceBridgeHandler,
    private val appHandler: AppBridgeHandler,
    private val permissionHandler: PermissionBridgeHandler,
    private val callerWebAppId: String,
    private val appScope: AppCoroutineScope
) : WebViewCompat.WebMessageListener {

    override fun onPostMessage(
        view: WebView,
        message: WebMessageCompat,
        sourceOrigin: Uri,
        isMainFrame: Boolean,
        replyProxy: JavaScriptReplyProxy
    ) {
        val rawData = message.data ?: return

        appScope.io.launch {
            val request = try {
                Json.decodeFromString(BridgeRequest.serializer(), rawData)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decode bridge request: ${e.message}", e)
                return@launch
            }

            val response = try {
                when (request.action) {
                    "getSurvey" -> surveyHandler.getSurvey(request)
                    "setSurveyResponse" -> surveyHandler.setSurveyResponse(request)
                    "getSensorData" -> sensorHandler.getSensorData(request)
                    "getLatestSensorReading" -> sensorHandler.getLatestSensorReading(request)
                    "getStorageData" -> storageHandler.get(request, callerWebAppId)
                    "setStorageData" -> storageHandler.set(request, callerWebAppId)
                    "getDeviceInfo" -> deviceHandler.getDeviceInfo(request)
                    "getWatchConnectionStatus" -> deviceHandler.getWatchConnectionStatus(request)
                    "checkPermissions" -> permissionHandler.checkPermissions(request)
                    "requestPermissions" -> permissionHandler.requestPermissions(request)
                    "showNativeToast" -> appHandler.showNativeToast(request)
                    "vibrate" -> appHandler.vibrate(request)
                    "scheduleLocalNotification" -> appHandler.scheduleLocalNotification(request)
                    "closeWebApp" -> appHandler.closeWebApp(request)
                    "openNativeSettings" -> appHandler.openNativeSettings(request)
                    else -> BridgeResponse(request.requestId, "error", errorMessage = "Unknown action: ${request.action}")
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "Bridge action '${request.action}' failed: ${e.message}", e)
                BridgeResponse(request.requestId, "error", errorMessage = e.message)
            }

            if (response != null) {
                replyProxy.postMessage(Json.encodeToString(BridgeResponse.serializer(), response))
            }
        }
    }

    companion object {
        private const val TAG = "EnPulseBridge"
    }
}
