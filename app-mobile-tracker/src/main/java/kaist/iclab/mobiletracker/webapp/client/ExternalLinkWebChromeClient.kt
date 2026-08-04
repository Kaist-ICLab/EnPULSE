package kaist.iclab.mobiletracker.webapp.client

import android.content.Intent
import android.net.Uri
import android.os.Message
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * Custom WebChromeClient that delegates target="_blank" (multiple windows) link creation
 * to the Android OS browser instead of attempting to host it inside the current app's task.
 */
class ExternalLinkWebChromeClient : WebChromeClient() {

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

    companion object {
        private const val TAG = "ExternalLinkChrome"
    }
}
