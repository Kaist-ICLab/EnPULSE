package kaist.iclab.mobiletracker.webapp

import android.content.Context
import kaist.iclab.mobiletracker.helpers.LanguageHelper
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebChromeClient
import android.os.Message
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
 * sensor / storage data via [EnPulseBridge].
 *
 * Uses `WebMessageListener` (not `addJavascriptInterface`) so the bridge is only reachable from the
 * webapp's own registered origin.
 *
 * Multi-WebApp Task Isolation:
 * Configured with `android:documentLaunchMode="intoExisting"` in `AndroidManifest.xml` alongside unique
 * data URIs (`webapp://$webAppId`) in launch intents so each WebApp runs in its own distinct task card
 * in the Android recents menu.
 */
class WebAppActivity : ComponentActivity(), KoinComponent {

    override fun attachBaseContext(newBase: Context) {
        val context = LanguageHelper(newBase).attachBaseContextWithLanguage(newBase)
        super.attachBaseContext(context)
    }

    private val webAppRegistry by inject<WebAppRegistry>()
    private val surveyBridgeHandler by inject<SurveyBridgeHandler>()
    private val sensorBridgeHandler by inject<SensorBridgeHandler>()
    private val storageBridgeHandler by inject<StorageBridgeHandler>()
    private val deviceBridgeHandler by inject<DeviceBridgeHandler>()
    private val permissionManager by inject<AndroidPermissionManager>()
    private val appScope by inject<AppCoroutineScope>()

    private lateinit var webView: WebView
    
    /** Queue of incoming permission requests to prevent dropping callbacks when requests overlap. */
    private val permissionRequestQueue = java.util.concurrent.ConcurrentLinkedQueue<Pair<List<String>, BridgeRequest>>()
    
    /** Currently active permission request awaiting user response. */
    private var activePermissionRequest: BridgeRequest? = null

    /**
     * Launcher for system permission dialogs. Encodes the permission result into JSON, converts it to
     * Base64, and dispatches a synthetic `MessageEvent` back to the WebView JS environment.
     */
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val request = activePermissionRequest
        if (request != null) {
            val responseData = buildJsonObject {
                result.forEach { (perm, isGranted) -> put(perm, isGranted) }
            }
            val response = BridgeResponse(request.requestId, "success", data = responseData)
            
            // Send response back to WebView securely via Base64 encoding to prevent JS string injection crashes
            if (::webView.isInitialized) {
                val jsonStr = Json.encodeToString(BridgeResponse.serializer(), response)
                val b64 = android.util.Base64.encodeToString(jsonStr.toByteArray(), android.util.Base64.NO_WRAP)
                val js = "window.dispatchEvent(new MessageEvent('message', { data: decodeURIComponent(escape(window.atob('$b64'))) }));"
                webView.evaluateJavascript(js, null)
            }
            activePermissionRequest = null
            processNextPermissionRequest()
        }
    }

    /**
     * Processes the next permission request in [permissionRequestQueue] if no request is currently active.
     */
    private fun processNextPermissionRequest() {
        if (activePermissionRequest != null) return
        val next = permissionRequestQueue.poll() ?: return
        activePermissionRequest = next.second
        requestPermissionLauncher.launch(next.first.toTypedArray())
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
            settings.supportMultipleWindows() // Enable support for multiple windows / target="_blank"
            webViewClient = RestrictedWebViewClient(webApp.allowedOrigin)
            setDownloadListener { downloadUrl, _, _, _, _ ->
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to delegate download URL to system browser: $downloadUrl", e)
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onCreateWindow(
                    view: WebView,
                    isDialog: Boolean,
                    isUserGesture: Boolean,
                    resultMsg: Message?
                ): Boolean {
                    // Try to resolve target="_blank" URL from requestNodeHref
                    val hrefMessage = view.handler.obtainMessage()
                    view.requestFocusNodeHref(hrefMessage)
                    val targetUrl = hrefMessage.data.getString("url")
                    if (!targetUrl.isNullOrEmpty()) {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                            view.context.startActivity(intent)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to launch external window for URL: $targetUrl", e)
                        }
                        return true
                    }
                    
                    // Fallback to transport webview intercepting redirects
                    val transport = resultMsg?.obj as? WebView.WebViewTransport
                    if (transport != null) {
                        val tempWebView = WebView(view.context)
                        tempWebView.webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                tempView: WebView,
                                request: WebResourceRequest
                            ): Boolean {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, request.url)
                                    tempView.context.startActivity(intent)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to launch external window from transport: ${request.url}", e)
                                }
                                return true
                            }
                        }
                        transport.webView = tempWebView
                        resultMsg.sendToTarget()
                        return true
                    }
                    return false
                }
            }
        }
        setContentView(webView)
        
        val appBridgeHandler = AppBridgeHandler(this) { finish() }
        
        val permissionBridgeHandler = PermissionBridgeHandler(this, permissionManager) { permissions, request ->
            permissionRequestQueue.offer(Pair(permissions, request))
            processNextPermissionRequest()
        }

        if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            val sanitizedOrigin = try {
                val uri = Uri.parse(webApp.allowedOrigin)
                val scheme = uri.scheme
                val host = uri.host
                if (!scheme.isNullOrEmpty() && !host.isNullOrEmpty()) {
                    val port = uri.port
                    if (port != -1) "$scheme://$host:$port" else "$scheme://$host"
                } else {
                    webApp.allowedOrigin.trimEnd('/')
                }
            } catch (e: Exception) {
                webApp.allowedOrigin.trimEnd('/')
            }

            try {
                WebViewCompat.addWebMessageListener(
                    webView,
                    BRIDGE_JS_OBJECT_NAME,
                    setOf(sanitizedOrigin),
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
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Failed to add web message listener for origin '$sanitizedOrigin': ${e.message}", e)
            }
        } else {
            Log.e(TAG, "WEB_MESSAGE_LISTENER unsupported on this WebView; native bridge disabled")
        }

        webView.loadUrl(url)
    }

    /**
     * Called when a new trigger/notification intent is delivered while this WebApp activity is already active
     * (e.g. brought to front via `documentLaunchMode="intoExisting"`).
     * Reloads the WebView with the new URL containing updated query parameters (such as `schedule_id`).
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Update the activity's intent
        val url = intent.getStringExtra(EXTRA_URL)
        if (url != null && ::webView.isInitialized) {
            Log.d(TAG, "Received new intent, dispatching custom JS event for URL: $url")
            try {
                val uri = Uri.parse(url)
                val paramsJson = buildJsonObject {
                    uri.queryParameterNames.forEach { name ->
                        put(name, uri.getQueryParameter(name) ?: "")
                    }
                    put("url", url)
                }
                
                val jsonStr = paramsJson.toString()
                val b64 = android.util.Base64.encodeToString(jsonStr.toByteArray(), android.util.Base64.NO_WRAP)
                val js = "window.dispatchEvent(new CustomEvent('enpulseNewIntent', { detail: JSON.parse(decodeURIComponent(escape(window.atob('$b64')))) }));"
                webView.evaluateJavascript(js, null)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to dispatch enpulseNewIntent event to WebView: ${e.message}", e)
                // Fallback to hard reload if event dispatching fails
                webView.loadUrl(url)
            }
        }
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
        private val TAG = WebAppActivity::class.simpleName
        const val EXTRA_URL = "url"
        const val EXTRA_WEBAPP_ID = "webAppId"
        const val BRIDGE_JS_OBJECT_NAME = "EnPulseNative"
    }
}

/**
 * Restricts WebView navigation to [allowedOrigin].
 * External links (different origins or custom intent schemes like `intent://`, `mailto:`, `tel:`)
 * are automatically delegated to the Android OS via [android.content.Intent.ACTION_VIEW] rather than rendering inside WebView.
 */
private class RestrictedWebViewClient(allowedOrigin: String) : WebViewClient() {
    private val allowedHost: String? = Uri.parse(allowedOrigin).host

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val scheme = request.url.scheme
        val host = request.url.host

        if ((scheme == "http" || scheme == "https") && host == allowedHost) {
            return false // Load internally in the WebView
        }

        // Delegate external links and custom schemes to the OS
        try {
            val intent = if (scheme == "intent") {
                android.content.Intent.parseUri(request.url.toString(), android.content.Intent.URI_INTENT_SCHEME)
            } else {
                android.content.Intent(android.content.Intent.ACTION_VIEW, request.url)
            }
            view.context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch external intent for ${request.url}", e)
        }

        return true
    }

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: android.webkit.WebResourceError
    ) {
        if (request.isForMainFrame) {
            val failedToLoad = view.context.getString(kaist.iclab.mobiletracker.R.string.webview_failed_to_load)
            val checkConnection = view.context.getString(kaist.iclab.mobiletracker.R.string.webview_check_connection)
            val htmlData = "<html><body style=\"display:flex;justify-content:center;align-items:center;height:100vh;flex-direction:column;\"><h2>$failedToLoad</h2><p>$checkConnection</p></body></html>"
            view.loadData(htmlData, "text/html", "UTF-8")
        }
        super.onReceivedError(view, request, error)
    }

    companion object {
        private const val TAG = "RestrictedWebViewClient"
    }
}
