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
    private val appScope by inject<AppCoroutineScope>()

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

        val webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            webViewClient = RestrictedWebViewClient(webApp.allowedOrigin)
        }
        setContentView(webView)

        if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            WebViewCompat.addWebMessageListener(
                webView,
                BRIDGE_JS_OBJECT_NAME,
                setOf(webApp.allowedOrigin),
                EnPulseBridge(
                    surveyHandler = surveyBridgeHandler,
                    sensorHandler = sensorBridgeHandler,
                    storageHandler = storageBridgeHandler,
                    callerWebAppId = webApp.id,
                    appScope = appScope
                )
            )
        } else {
            Log.e(TAG, "WEB_MESSAGE_LISTENER unsupported on this WebView; native bridge disabled")
        }

        webView.loadUrl(url)
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
        val host = request.url.host
        if (host != null && host == allowedHost) return false

        Log.w(TAG, "Blocked navigation to disallowed origin: ${request.url}")
        return true
    }

    companion object {
        private const val TAG = "RestrictedWebViewClient"
    }
}
