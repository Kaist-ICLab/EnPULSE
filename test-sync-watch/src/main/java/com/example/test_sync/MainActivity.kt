package com.example.test_sync

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import kaist.iclab.tracker.sync.ble.BLEDataChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement

@Serializable
data class TestData(
    val message: String,
    val value: Int,
)

class MainActivity : ComponentActivity() {
    // Complete BLE Channel (symmetric - both send and receive)
    private lateinit var bleChannel: BLEDataChannel

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Initialize BLE components after context is available
        initializeBLEComponents()

        // Set up listeners for the complete channel
        setupBLEChannelListeners()

        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            WearApp(
                // Complete BLE Channel examples
                sendStringOverBLE = { key, value ->
                    CoroutineScope(Dispatchers.IO).launch {
                        Log.d(
                            "WATCH_BLE_SEND",
                            "⌚ Sending message to phone - Key: '$key', Data: $value"
                        )
                        bleChannel.send(key, value)
                    }
                },
                sendTestDataOverBLE = { key, value ->
                    CoroutineScope(Dispatchers.IO).launch {
                        val jsonString = Json.encodeToString(value)
                        Log.d(
                            "WATCH_BLE_SEND",
                            "⌚ Sending structured data to phone - Key: '$key', Data: $jsonString"
                        )
                        bleChannel.send(key, jsonString)
                    }
                },
            )
        }
    }

    private fun initializeBLEComponents() {
        // Initialize BLE components after context is available
        bleChannel = BLEDataChannel(this)
    }

    private fun setupBLEChannelListeners() {
        // Listen for simple string messages
        bleChannel.addOnReceivedListener(setOf("message")) { key, json ->
            val message = when {
                json is kotlinx.serialization.json.JsonPrimitive -> json.content
                else -> json.toString()
            }
            Log.d(
                "WATCH_BLE_CHANNEL",
                "⌚ Received message from phone - Key: '$key', Data: $message"
            )
        }

        // Listen for structured data
        bleChannel.addOnReceivedListener(setOf("structured_data")) { key, json ->
            val testData: TestData = Json.decodeFromJsonElement(json)
            Log.d(
                "WATCH_BLE_CHANNEL",
                "⌚ Received structured data from phone - Key: '$key', Data: $testData"
            )
        }

    }
}

@Composable
fun WearApp(
    sendStringOverBLE: (String, String) -> Unit,
    sendTestDataOverBLE: (String, TestData) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "⌚ Watch BLE Test",
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )

            Text(
                text = "BLE Watch <-> Phone",
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            )

            Button(
                onClick = { sendStringOverBLE("message", "HELLO STRING FROM WATCH") },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(40.dp)
                    .padding(vertical = 2.dp)
            ) {
                Text("Send String")
            }

            Button(
                onClick = {
                    sendTestDataOverBLE(
                        "structured_data",
                        TestData(message = "HELLO STRUCTURED DATA FROM WATCH", value = 456)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(40.dp)
                    .padding(vertical = 2.dp)
            ) {
                Text("Send Structured Data")
            }

        }
    }
}
