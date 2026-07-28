package kaist.iclab.mobiletracker.webapp

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import kaist.iclab.mobiletracker.di.AppCoroutineScope
import kaist.iclab.mobiletracker.webapp.bridge.EnPulseBridge
import kaist.iclab.mobiletracker.webapp.bridge.SensorBridgeHandler
import kaist.iclab.mobiletracker.webapp.bridge.StorageBridgeHandler
import kaist.iclab.mobiletracker.webapp.bridge.SurveyBridgeHandler
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import androidx.activity.result.contract.ActivityResultContracts
import kaist.iclab.mobiletracker.webapp.bridge.AppBridgeHandler
import kaist.iclab.mobiletracker.webapp.bridge.BridgeRequest
import kaist.iclab.mobiletracker.webapp.bridge.BridgeResponse
import kaist.iclab.mobiletracker.webapp.bridge.DeviceBridgeHandler
import kaist.iclab.mobiletracker.webapp.bridge.PermissionBridgeHandler
import kaist.iclab.tracker.permission.AndroidPermissionManager
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * WebView container that hosts a third-party EnPULSE webapp and bridges it to native survey /
 * sensor / storage data via [EnPulseBridge]. Uses `WebMessageListener` (not `addJavascriptInterface`)
 * so the bridge is only reachable from the webapp's own registered origin.
 */
class WebViewSurveyActivity : ComponentActivity(), KoinComponent {

    private val webAppRegistry by inject<WebAppRegistry>()
    private val surveyBridgeHandler by inject<SurveyBridgeHandler>()
    private val sensorBridgeHandler by inject<SensorBridgeHandler>()
    private val storageBridgeHandler by inject<StorageBridgeHandler>()
    private val deviceBridgeHandler by inject<DeviceBridgeHandler>()
    private val permissionManager by inject<AndroidPermissionManager>()
    private val appScope by inject<AppCoroutineScope>()

    private lateinit var webView: WebView
    
    // For mapping active permission requests back to the Bridge Request ID
    private var activePermissionRequest: BridgeRequest? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val request = activePermissionRequest
        if (request != null) {
            val responseData = buildJsonObject {
                result.forEach { (perm, isGranted) -> put(perm, isGranted) }
            }
            val response = BridgeResponse(request.requestId, "success", data = responseData)
            
            // Send response back to WebView
            if (::webView.isInitialized) {
                // Find the bridge proxy to send it. Since we can't easily get the ReplyProxy here,
                // we dispatch via JS. We used addWebMessageListener, so we can't just `postMessage` easily from the outside.
                // Wait, WebMessageListener replies must be sent via JavaScriptReplyProxy. 
                // Alternatively, we can inject a small JS function to receive async pushes.
                // But a cleaner way: evaluateJavascript to dispatch a MessageEvent to the EnPulseNative.
                val jsonStr = Json.encodeToString(BridgeResponse.serializer(), response).replace("'", "\\'")
                webView.evaluateJavascript("window.dispatchEvent(new MessageEvent('message', { data: '$jsonStr' }));", null)
            }
            activePermissionRequest = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = intent.getStringExtra(EXTRA_URL)
        val webAppId = intent.getStringExtra(EXTRA_WEBAPP_ID)
        val webApp = webAppId?.let { webAppRegistry.get(it) }

        if (url == null || webApp == null) {
            Log.e(TAG, "Missing url or unknown webAppId=$webAppId; closing")
            finish()
            return
        }

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = RestrictedWebViewClient(webApp.allowedOrigin)
        }
        setContentView(webView)
        
        val appBridgeHandler = AppBridgeHandler(this) { finish() }
        
        val permissionBridgeHandler = PermissionBridgeHandler(this, permissionManager) { permissions, request ->
            activePermissionRequest = request
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }

        if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            WebViewCompat.addWebMessageListener(
                webView,
                BRIDGE_JS_OBJECT_NAME,
                setOf(webApp.allowedOrigin),
                EnPulseBridge(
                    surveyHandler = surveyBridgeHandler,
                    sensorHandler = sensorBridgeHandler,
                    storageHandler = storageBridgeHandler,
                    deviceHandler = deviceBridgeHandler,
                    appHandler = appBridgeHandler,
                    permissionHandler = permissionBridgeHandler,
                    callerWebAppId = webApp.id,
                    appScope = appScope
                )
            )
        } else {
            Log.e(TAG, "WEB_MESSAGE_LISTENER unsupported on this WebView; native bridge disabled")
        }

        webView.loadUrl(url)
    }

    override fun onPause() {
        super.onPause()
        if (::webView.isInitialized) {
            webView.onPause()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::webView.isInitialized) {
            webView.onResume()
        }
    }

    override fun onDestroy() {
        if (::webView.isInitialized) {
            webView.destroy()
        }
        super.onDestroy()
    }

    companion object {
        private val TAG = WebViewSurveyActivity::class.simpleName
        const val EXTRA_URL = "url"
        const val EXTRA_WEBAPP_ID = "webAppId"
        const val BRIDGE_JS_OBJECT_NAME = "EnPulseNative"
    }
}

/**
 * Blocks navigation away from [allowedOrigin] so a compromised or malicious page loaded inside
 * the webapp (e.g. via a redirect) can't smuggle the user to an untrusted origin that would still
 * render inside this trusted webapp chrome.
 */
private class RestrictedWebViewClient(allowedOrigin: String) : WebViewClient() {
    private val allowedHost: String? = Uri.parse(allowedOrigin).host

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val scheme = request.url.scheme
        if (scheme != "http" && scheme != "https") {
            Log.w(TAG, "Blocked non-http(s) scheme navigation: ${request.url}")
            return true
        }

        val host = request.url.host
        if (host != null && host == allowedHost) return false

        Log.w(TAG, "Blocked navigation to disallowed origin: ${request.url}")
        return true
    }

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: android.webkit.WebResourceError
    ) {
        if (request.isForMainFrame) {
            val htmlData = "<html><body style=\"display:flex;justify-content:center;align-items:center;height:100vh;flex-direction:column;font-family:sans-serif;\"><h2>Failed to load</h2><p>Please check your connection and try again.</p></body></html>"
            view.loadData(htmlData, "text/html", "UTF-8")
        }
        super.onReceivedError(view, request, error)
    }

    companion object {
        private const val TAG = "RestrictedWebViewClient"
    }
}
