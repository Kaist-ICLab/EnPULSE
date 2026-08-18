package kaist.iclab.mobiletracker.webapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import kaist.iclab.mobiletracker.helpers.LanguageHelper

/**
 * Minimal in-app browser for arbitrary URLs coming from a `notification` trigger action —
 * whether shown directly on the phone or relayed back from a tapped watch notification (see
 * [kaist.iclab.mobiletracker.helpers.BLEHelper] `handleOpenUrlTrigger`).
 *
 * Deliberately **not** [WebAppActivity]: that one exposes [kaist.iclab.mobiletracker.webapp.bridge.EnPulseBridge]
 * (native sensor/storage/device access) and only loads URLs matching a registered WebApp's
 * allowed origin. A `notification` action's `url` is dashboard-authored, arbitrary, untrusted
 * input with no associated WebApp registration — giving it bridge access would be a real
 * phishing/data-exfiltration risk. This activity just renders the page, nothing more.
 */
class SimpleWebViewActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val context = LanguageHelper(newBase).attachBaseContextWithLanguage(newBase)
        super.attachBaseContext(context)
    }

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = intent.getStringExtra(EXTRA_URL)
        if (url == null) {
            Log.e(TAG, "Missing url; closing")
            finish()
            return
        }

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            webViewClient = WebViewClient()
            setDownloadListener { downloadUrl, _, _, _, _ ->
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)))
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to delegate download URL to system browser: $downloadUrl", e)
                }
            }
        }
        setContentView(webView)
        webView.loadUrl(url)

        onBackPressedDispatcher.addCallback(this) {
            if (::webView.isInitialized && webView.canGoBack()) {
                webView.goBack()
            } else {
                finish()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (::webView.isInitialized) webView.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (::webView.isInitialized) webView.onResume()
    }

    override fun onDestroy() {
        if (::webView.isInitialized) webView.destroy()
        super.onDestroy()
    }

    companion object {
        private val TAG = SimpleWebViewActivity::class.simpleName
        const val EXTRA_URL = "url"
    }
}
