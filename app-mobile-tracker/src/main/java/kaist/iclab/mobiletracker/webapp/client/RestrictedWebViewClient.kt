package kaist.iclab.mobiletracker.webapp.client

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * Restricts WebView navigation to [allowedOrigin].
 * External links (different origins or custom intent schemes like `intent://`, `mailto:`, `tel:`)
 * are automatically delegated to the Android OS via [Intent.ACTION_VIEW] rather than rendering inside WebView.
 */
class RestrictedWebViewClient(
    allowedOrigin: String,
    private val onExternalRedirect: () -> Unit = {}
) : WebViewClient() {
    private val allowedHost: String? = Uri.parse(allowedOrigin).host

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val scheme = request.url.scheme
        val host = request.url.host

        val isAllowedHost = host == allowedHost || (allowedHost != null && host?.endsWith(".$allowedHost") == true)
        if ((scheme == "http" || scheme == "https") && isAllowedHost) {
            return false // Load internally in the WebView
        }

        // Delegate external links and custom schemes to the OS
        try {
            val intent = if (scheme == "intent") {
                Intent.parseUri(request.url.toString(), Intent.URI_INTENT_SCHEME)
            } else {
                Intent(Intent.ACTION_VIEW, request.url)
            }
            view.context.startActivity(intent)

            if (request.isForMainFrame) {
                onExternalRedirect()
            }
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
