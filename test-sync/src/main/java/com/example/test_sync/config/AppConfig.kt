package com.example.test_sync.config

/**
 * Configuration file for test-sync app.
 * Contains all implemented configuration variables.
 *
 * ⚠️ IMPORTANT: Replace placeholder values with your actual configuration!
 */
object AppConfig {

    // ==================== SUPABASE CONFIGURATION ====================

    /**
     * Supabase project URL
     * Get this from your Supabase project dashboard > Settings > API
     * Example: https://your-project-id.supabase.co
     */
    const val SUPABASE_URL = "https://urtvamywsnujfmcdnnxb.supabase.co"

    /**
     * Supabase anonymous/public key
     * Get this from your Supabase project settings > API > Project API keys
     * This is safe to use in client applications
     */
    const val SUPABASE_ANON_KEY =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVydHZhbXl3c251amZtY2RubnhiIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTA5MTExMDYsImV4cCI6MjA2NjQ4NzEwNn0.VIGwIg_IRqJuw2Vqqk8SM8raWC2AjryMSZSwnTrmBaI"

    /**
     * Supabase table name for test data
     * Make sure this table exists in your Supabase database
     * Required columns: id (serial), message (text), value (integer), created_at (timestamp)
     */
    const val SUPABASE_TABLE_NAME = "test_data"

    /**
     * Polling configuration for database monitoring
     * Controls how often the app checks for new data in the database
     */
    object Polling {
        /**
         * Polling interval in milliseconds
         * How often to check the database for new data (5 seconds = 5000ms)
         */
        const val INTERVAL_MS = 5000L

        /**
         * Retry delay in milliseconds when polling fails
         * How long to wait before retrying after an error (5 seconds = 5000ms)
         */
        const val RETRY_DELAY_MS = 5000L
    }

    // ==================== INTERNET/HTTP CONFIGURATION ====================

    /**
     * HTTPBin URL for testing HTTP requests
     * HTTPBin is a simple HTTP request & response service for testing
     */
    const val HTTPBIN_URL = "https://httpbin.org"

    // ==================== BLE CONFIGURATION ====================

    /**
     * BLE message keys for different data types
     * These keys are used to identify different types of messages in BLE communication
     */
    object BLEKeys {
        const val MESSAGE = "message"
        const val STRUCTURED_DATA = "structured_data"
        const val SENSOR_DATA_CSV = "sensor_data_csv"
    }

    // ==================== LOGGING CONFIGURATION ====================

    /**
     * Log tags for different components
     * Use these tags to filter logs: adb logcat | grep "TAG_NAME"
     */
    object LogTags {
        const val PHONE_BLE = "PHONE_BLE"
        const val PHONE_INTERNET = "PHONE_INTERNET"
        const val PHONE_SUPABASE = "PHONE_SUPABASE"
    }

}
