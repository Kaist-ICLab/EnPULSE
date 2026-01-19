package com.example.test_sync.helpers

import android.util.Log
import com.example.test_sync.config.AppConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.Realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class SupabaseData(
    val id: Int? = null,
    val message: String,
    val value: Int,
    val created_at: String? = null
)

/**
 * Direct Supabase integration using supabase-kt library
 */
class SupabaseHelper {
    private val supabaseClient = createSupabaseClient(
        supabaseUrl = AppConfig.SUPABASE_URL,
        supabaseKey = AppConfig.SUPABASE_ANON_KEY
    ) {
        install(Postgrest)  // Database operations
        install(Realtime)  // Real-time subscriptions
    }
    
    // Polling state
    private var isPollingActive = false
    private var lastSeenDataId: Int? = null  // Track last seen data to detect new changes

    fun sendData(message: String, value: Int) {
        Log.d(AppConfig.LogTags.PHONE_SUPABASE, "Sending data: Message='$message', Value=$value")
        val data = SupabaseData(
            message = message,
            value = value
        )
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = supabaseClient.from(AppConfig.SUPABASE_TABLE_NAME).insert(data)
                Log.d(AppConfig.LogTags.PHONE_SUPABASE, "Data sent successfully")
                try {
                    val responseData = response.decodeList<SupabaseData>()
                    Log.d(AppConfig.LogTags.PHONE_SUPABASE, "Response: $responseData")
                } catch (e: Exception) {
                    Log.d(AppConfig.LogTags.PHONE_SUPABASE, "Response: $response")
                }
            } catch (e: Exception) {
                Log.e(AppConfig.LogTags.PHONE_SUPABASE, "Error sending data: ${e.message}")
            }
        }
    }


    fun getData() {
        Log.d(AppConfig.LogTags.PHONE_SUPABASE, "Retrieving data from Supabase")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = supabaseClient.from(AppConfig.SUPABASE_TABLE_NAME).select()
                Log.d(AppConfig.LogTags.PHONE_SUPABASE, "Data retrieved successfully")
                try {
                    val responseData = response.decodeList<SupabaseData>()
                    Log.d(AppConfig.LogTags.PHONE_SUPABASE, "Response: $responseData")
                } catch (e: Exception) {
                    Log.d(AppConfig.LogTags.PHONE_SUPABASE, "Response: $response")
                }
            } catch (e: Exception) {
                Log.e(AppConfig.LogTags.PHONE_SUPABASE, "Error fetching data: ${e.message}")
            }
        }
    }
    
    /**
     * Start real-time polling for database changes
     * Uses polling to check for database changes every 5 seconds
     * This is a reliable approach that works with all Supabase versions
     */
    fun startPolling() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                isPollingActive = true
                Log.d(AppConfig.LogTags.PHONE_SUPABASE, "Polling ON")
                startDatabasePolling()
            } catch (e: Exception) {
                Log.e(AppConfig.LogTags.PHONE_SUPABASE, "Error starting polling: ${e.message}")
            }
        }
    }
    
    /**
     * Stop real-time polling
     * Stops the polling process that checks for database changes
     */
    fun stopPolling() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                isPollingActive = false
                Log.d(AppConfig.LogTags.PHONE_SUPABASE, "Polling OFF")
            } catch (e: Exception) {
                Log.e(AppConfig.LogTags.PHONE_SUPABASE, "Error stopping polling: ${e.message}")
            }
        }
    }
    
    /**
     * Start database polling to check for changes
     * This polls the Supabase database every 5 seconds for new data
     */
    private fun startDatabasePolling() {
        CoroutineScope(Dispatchers.IO).launch {
            while (isPollingActive) {
                try {
                    val response = supabaseClient.from(AppConfig.SUPABASE_TABLE_NAME).select()
                    if (isPollingActive) {
                        try {
                            val dataList = response.decodeList<SupabaseData>()
                            
                            if (dataList.isNotEmpty()) {
                                val latestData = dataList.last()
                                
                                if (latestData.id != lastSeenDataId) {
                                    Log.d(AppConfig.LogTags.PHONE_SUPABASE, "Received data: ID=${latestData.id}, Message='${latestData.message}', Value=${latestData.value}")
                                    lastSeenDataId = latestData.id
                                }
                            }
                        } catch (parseError: Exception) {
                            Log.e(AppConfig.LogTags.PHONE_SUPABASE, "Error parsing data: ${parseError.message}")
                        }
                    }
                    
                    kotlinx.coroutines.delay(AppConfig.Polling.INTERVAL_MS)
                    
                } catch (e: Exception) {
                    Log.e(AppConfig.LogTags.PHONE_SUPABASE, "Error in polling: ${e.message}")
                    kotlinx.coroutines.delay(AppConfig.Polling.RETRY_DELAY_MS)
                }
            }
        }
    }
    
    
    /**
     * Get current polling status
     */
    fun isPollingActive(): Boolean {
        return isPollingActive
    }
}