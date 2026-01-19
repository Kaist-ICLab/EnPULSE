package com.example.test_sync.helpers

import android.util.Log
import com.example.test_sync.TestData
import com.example.test_sync.config.AppConfig
import kaist.iclab.tracker.sync.internet.InternetDataChannel
import kaist.iclab.tracker.sync.internet.InternetMethod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class InternetHelper {
    private val internetChannel = InternetDataChannel()

    fun sendGetRequest(url: String = AppConfig.HTTPBIN_URL) {
        CoroutineScope(Dispatchers.IO).launch {
            Log.d(AppConfig.LogTags.PHONE_INTERNET, "🌐 Sending GET request to server - URL: '$url'")
            try {
                val response = internetChannel.send(url, "", InternetMethod.GET)
                val responseBody = response.body?.string() ?: "No response body"
                Log.d(AppConfig.LogTags.PHONE_INTERNET, "GET Response (${response.code}): $responseBody")
            } catch (e: Exception) {
                Log.e(AppConfig.LogTags.PHONE_INTERNET, "GET Error: ${e.message}")
            }
        }
    }

    fun sendPostRequest(url: String = AppConfig.HTTPBIN_URL, value: TestData) {
        CoroutineScope(Dispatchers.IO).launch {
            val jsonString = Json.encodeToString(value)
            Log.d(AppConfig.LogTags.PHONE_INTERNET, "🌐 Sending POST request to server - URL: '$url', Data: $jsonString")
            try {
                val response = internetChannel.send(url, jsonString, InternetMethod.POST)
                val responseBody = response.body?.string() ?: "No response body"
                Log.d(AppConfig.LogTags.PHONE_INTERNET, "POST Response (${response.code}): $responseBody")
            } catch (e: Exception) {
                Log.e(AppConfig.LogTags.PHONE_INTERNET, "POST Error: ${e.message}")
            }
        }
    }
}
