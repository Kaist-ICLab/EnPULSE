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
import android.webkit.WebSettings
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import kaist.iclab.mobiletracker.di.AppCoroutineScope
import kaist.iclab.mobiletracker.webapp.bridge.EnPulseBridge
import kaist.iclab.mobiletracker.webapp.bridge.SensorBridgeHandler
import kaist.iclab.mobiletracker.webapp.bridge.StorageBridgeHandler
import kaist.iclab.mobiletracker.webapp.bridge.SurveyBridgeHandler
import kaist.iclab.mobiletracker.webapp.client.RestrictedWebViewClient
import kaist.iclab.mobiletracker.webapp.client.ExternalLinkWebChromeClient
import kaist.iclab.mobiletracker.webapp.client.WebAppPermissionHelper
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import androidx.activity.result.contract.ActivityResultContracts
import kaist.iclab.mobiletracker.webapp.bridge.AppBridgeHandler
import kaist.iclab.mobiletracker.webapp.bridge.BridgeRequest
import kaist.iclab.mobiletracker.webapp.bridge.BridgeResponse
import kaist.iclab.mobiletracker.webapp.bridge.DeviceBridgeHandler
import kaist.iclab.mobiletracker.webapp.bridge.LogBridgeHandler
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
    private val logBridgeHandler by inject<LogBridgeHandler>()
    private val permissionManager by inject<AndroidPermissionManager>()
    private val appScope by inject<AppCoroutineScope>()

    private lateinit var webView: WebView
    
    private val permissionHelper = WebAppPermissionHelper(this) { b64 ->
        if (::webView.isInitialized) {
            val js = "window.dispatchEvent(new MessageEvent('message', { data: decodeURIComponent(escape(window.atob('$b64'))) }));"
            webView.evaluateJavascript(js, null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val originalUrl = intent.getStringExtra(EXTRA_URL)
        val webAppId = intent.getStringExtra(EXTRA_WEBAPP_ID)
        val webApp = webAppId?.let { webAppRegistry.get(it) }

        if (originalUrl == null || webApp == null) {
            Log.e(TAG, "Missing url or unknown webAppId=$webAppId; closing")
            finish()
            return
        }

        var url = originalUrl

        // Validate URL host against allowedOrigin to prevent phishing/spoofing attacks from malicious intents
        val intentHost = try { Uri.parse(url).host } catch (e: Exception) { null }
        val allowedHost = try { Uri.parse(webApp.allowedOrigin).host } catch (e: Exception) { null }
        
        if (intentHost == null || allowedHost == null || intentHost != allowedHost) {
            Log.e(TAG, "Security Warning: Requested URL host ($intentHost) does not match allowed origin ($allowedHost). Falling back to registered URL.")
            url = webApp.url
        }

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
            settings.supportMultipleWindows() // Enable support for multiple windows / target="_blank"
            webViewClient = RestrictedWebViewClient(webApp.allowedOrigin) {
                if (!canGoBack()) {
                    finish()
                }
            }
            setDownloadListener { downloadUrl, _, _, _, _ ->
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to delegate download URL to system browser: $downloadUrl", e)
                }
            }
            webChromeClient = ExternalLinkWebChromeClient()
        }
        setContentView(webView)
        
        val appBridgeHandler = AppBridgeHandler(this) { finish() }
        
        val permissionBridgeHandler = PermissionBridgeHandler(this, permissionManager) { permissions, request ->
            permissionHelper.requestPermissions(permissions, request)
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
                        logHandler = logBridgeHandler,
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
        val originalUrl = intent.getStringExtra(EXTRA_URL)
        val webAppId = intent.getStringExtra(EXTRA_WEBAPP_ID)
        val webApp = webAppId?.let { webAppRegistry.get(it) }

        var url = originalUrl
        if (url != null && webApp != null) {
            val intentHost = try { Uri.parse(url).host } catch (e: Exception) { null }
            val allowedHost = try { Uri.parse(webApp.allowedOrigin).host } catch (e: Exception) { null }
            if (intentHost == null || allowedHost == null || intentHost != allowedHost) {
                Log.e(TAG, "Security Warning: Requested URL host ($intentHost) does not match allowed origin ($allowedHost). Falling back to registered URL.")
                url = webApp.url
            }
        }

        if (url != null && ::webView.isInitialized) {
            Log.d(TAG, "Received new intent, reloading WebView with new URL: $url")
            webView.loadUrl(url)
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

