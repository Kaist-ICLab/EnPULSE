package com.example.test_sync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.test_sync.helpers.BLEHelper
import com.example.test_sync.helpers.InternetHelper
import com.example.test_sync.helpers.SupabaseHelper
import com.example.test_sync.ui.theme.AndroidtrackerTheme

class MainActivity : ComponentActivity() {
    // Helper classes for cleaner separation
    private lateinit var bleHelper: BLEHelper
    private lateinit var internetHelper: InternetHelper
    private lateinit var supabaseHelper: SupabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize helpers
        bleHelper = BLEHelper(this)
        bleHelper.initialize()

        internetHelper = InternetHelper()
        supabaseHelper = SupabaseHelper()

        setContent {
            AndroidtrackerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        // BLE Communication
                        sendStringOverBLE = bleHelper::sendString,
                        sendTestDataOverBLE = bleHelper::sendTestData,

                        // Internet Communication
                        sendGetRequest = internetHelper::sendGetRequest,
                        sendPostRequest = internetHelper::sendPostRequest,

                        // Supabase Communication
                        sendToSupabase = supabaseHelper::sendData,
                        getFromSupabase = supabaseHelper::getData,

                        // Data Polling Control
                        togglePolling = ::togglePolling,

                        // Style Modifier
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                    )
                }
            }
        }
    }

    /**
     * Toggle polling
     */
    private fun togglePolling() {
        if (supabaseHelper.isPollingActive()) {
            supabaseHelper.stopPolling()
        } else {
            supabaseHelper.startPolling()
        }
    }
}