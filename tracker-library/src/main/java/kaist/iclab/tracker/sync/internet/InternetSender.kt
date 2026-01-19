package kaist.iclab.tracker.sync.internet

import kaist.iclab.tracker.sync.core.DataChannelSender
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

/**
 * Internet data sender for sending data through HTTP.
 * Supports various HTTP methods for different use cases.
 *
 * Internal class - only accessible through InternetDataChannel.
 */
enum class InternetMethod {
    GET,
    POST,
    PUT,
    PATCH,
    DELETE
}

internal class InternetSender : DataChannelSender<Response>() {

    private val client = OkHttpClient()

    override suspend fun send(key: String, value: String): Response {
        return send(key, value, InternetMethod.POST)
    }

    fun send(key: String, value: String, method: InternetMethod): Response {
        val url = key
        val jsonMediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = value.toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url(url)
            .apply {
                when (method) {
                    InternetMethod.GET -> get()
                    InternetMethod.POST -> post(requestBody)
                    InternetMethod.PUT -> put(requestBody)
                    InternetMethod.PATCH -> patch(requestBody)
                    InternetMethod.DELETE -> delete(requestBody)
                }
            }
            .build()

        return client.newCall(request).execute()
    }
}
