package kaist.iclab.mobiletracker.config

/**
 * TODO: Change approach using environment variables
 * Configuration file for test-sync app.
 * Contains all implemented configuration variables.
 * 
 * ⚠️ IMPORTANT: Replace placeholder values with your actual configuration!
 */
object AppConfig {
    /**
     * Supabase project URL
     * Get this from your Supabase project dashboard > Settings > API
     * Example: https://your-project-id.supabase.co
     */
    const val SUPABASE_URL = "http://143.248.57.5:8000/"
    
    /**
     * Supabase anonymous/public key
     * Get this from your Supabase project settings > API > Project API keys
     * This is safe to use in client applications
     */
    const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJyb2xlIjoiYW5vbiIsImlzcyI6InN1cGFiYXNlIiwiaWF0IjoxNzYyODczMjAwLCJleHAiOjE5MjA2Mzk2MDB9.D-Hqc9yhQJo6NHkBSllMzu-P435ay6-L_JYhEfO58TQ"

    /**
     * Supabase table names for sensor data
     * Centralized location for all Supabase table names used in the application
     */
    object SupabaseTables {
        // Watch sensor table names
        const val LOCATION_SENSOR = "location_sensor"
        const val ACCELEROMETER_SENSOR = "accelerometer_sensor"
        const val EDA_SENSOR = "eda_sensor"
        const val HEART_RATE_SENSOR = "heart_rate_sensor"
        const val PPG_SENSOR = "ppg_sensor"
        const val SKIN_TEMPERATURE_SENSOR = "skin_temperature_sensor"

        // Phone sensor table names
        const val AMBIENT_LIGHT_SENSOR = "ambient_light_sensor"
        const val APP_LIST_CHANGE_SENSOR = "app_list_change_sensor"
        const val APP_USAGE_LOG_SENSOR = "app_usage_log_sensor"
        const val MESSAGE_LOG_SENSOR = "message_log_sensor"
        const val USER_INTERACTION_SENSOR = "user_interaction_sensor"
        const val BATTERY_SENSOR = "battery_sensor"
        const val BLUETOOTH_SCAN_SENSOR = "bluetooth_scan_sensor"
        const val CALL_LOG_SENSOR = "call_log_sensor"
        const val CONNECTIVITY_SENSOR = "connectivity_sensor"
        const val DATA_TRAFFIC_SENSOR = "data_traffic_sensor"
        const val DEVICE_MODE_SENSOR = "device_mode_sensor"
        const val MEDIA_SENSOR = "media_sensor"
        const val NOTIFICATION_SENSOR = "notification_sensor"
        const val STEP_SENSOR = "step_sensor"
        const val SCREEN_SENSOR = "screen_sensor"
        const val WIFI_SCAN_SENSOR = "wifi_scan_sensor"
    }

    /**
     * BLE message keys for different data types
     * These keys are used to identify different types of messages in BLE communication
     */
    object BLEKeys {
        const val SENSOR_DATA_CSV = "sensor_data_csv"
        const val SYNC_ACK = "sync_ack"
    }

    /**
     * Log tags for different components
     * Use these tags to filter logs: adb logcat | grep "TAG_NAME"
     */
    object LogTags {
        const val PHONE_BLE = "PHONE_BLE"
        const val PHONE_SUPABASE = "PHONE_SUPABASE"
    }
}
