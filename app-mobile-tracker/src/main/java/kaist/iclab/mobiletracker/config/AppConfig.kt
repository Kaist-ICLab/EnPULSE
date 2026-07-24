package kaist.iclab.mobiletracker.config

import kaist.iclab.mobiletracker.BuildConfig

/**
 * Environment-specific configuration for the mobile tracker app.
 *
 * This file contains values that vary by environment or external integration:
 * - Supabase credentials (URL, keys) injected via BuildConfig
 * - Supabase remote table names
 * - BLE communication keys
 * - Logging tags for debugging
 *
 * For internal app constants (DB settings, Prefs keys, Intervals),
 * see [kaist.iclab.mobiletracker.Constants].
 */
object AppConfig {
    /**
     * Supabase project URL
     * Injected from local.properties (SUPABASE_URL) or environment variable
     */
    const val SUPABASE_URL: String = BuildConfig.SUPABASE_URL

    /**
     * Supabase anonymous/public key
     * Injected from local.properties (SUPABASE_ANON_KEY) or environment variable
     * This is safe to use in client applications
     */
    const val SUPABASE_ANON_KEY: String = BuildConfig.SUPABASE_ANON_KEY

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
        const val IMU_SENSOR = "imu_sensor"
        const val GESTURE_SENSOR = "gesture_sensor"
        const val STRESS_SENSOR = "stress_sensor"
        const val ECG_SENSOR = "ecg_sensor"

        // Phone sensor table names
        const val ACTIVITY_RECOGNITION_SENSOR = "activity_recognition_sensor"
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
        const val EXERCISE_SENSOR = "exercise_sensor"
        const val SLEEP_SENSOR = "sleep_sensor"
    }

    /**
     * BLE message keys for different data types
     * These keys are used to identify different types of messages in BLE communication
     */
    object BLEKeys {
        const val SENSOR_DATA_CSV = "sensor_data_csv"
        const val SYNC_ACK = "sync_ack"
        const val MICRO_EMA_RESPONSE = "micro_ema_response"
        const val MICRO_EMA_ACK = "micro_ema_ack"
        const val MICRO_EMA_TRIGGER = "micro_ema_trigger"
        const val TRIGGER_CONFIG = "trigger_config"
        const val PHONE_EMA_TRIGGER = "phone_ema_trigger"
        const val DETECTION_STATE_UPDATE = "detection_state_update"
        const val ACTIVE_SENSOR_CONFIG = "active_sensor_config"
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
