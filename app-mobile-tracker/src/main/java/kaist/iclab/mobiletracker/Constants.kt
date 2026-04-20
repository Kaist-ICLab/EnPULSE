package kaist.iclab.mobiletracker

/**
 * Centralized constants for the mobile tracker app.
 *
 * This file contains internal app logic constants such as:
 * - Database table names and batch sizes
 * - Shared Preferences keys
 * - Synchronization intervals and network types
 * - Notification channel IDs and names
 * - Internal sensor identifiers used for logic
 *
 * For environment-specific values like Supabase URLs and API keys,
 * see [kaist.iclab.mobiletracker.config.AppConfig].
 */
object Constants {
    /**
     * Database Constants
     */
    object DB {
        const val BUFFER_SIZE = 1000
        const val BATCH_SIZE = 50
        const val FLUSH_INTERVAL_MS = 5000L

        // Survey Tables
        const val TABLE_SURVEY = "survey"
        const val TABLE_QUESTION = "survey_question"
        const val TABLE_OPTION = "survey_question_option"
        const val TABLE_TRIGGER = "survey_question_trigger"
        const val TABLE_RESPONSE = "survey_question_response"
    }

    /**
     * Shared Preferences Constants
     */
    object Prefs {
        const val PREFS_NAME = "language_preferences"
        const val SYNC_PREFS_NAME = "sync_preferences"
        const val KEY_LANGUAGE = "selected_language"

        // Auto Sync Prefs
        const val KEY_LAST_WATCH_DATA = "last_watch_data"
        const val KEY_LAST_PHONE_SENSOR = "last_phone_sensor"
        const val KEY_LAST_SUCCESSFUL_UPLOAD = "last_successful_upload"
        const val KEY_DATA_COLLECTION_STARTED = "data_collection_started"
        const val KEY_AUTO_SYNC_INTERVAL = "auto_sync_interval"
        const val KEY_AUTO_SYNC_NETWORK = "auto_sync_network"
        const val KEY_CACHED_USER_UUID = "cached_user_uuid"
    }

    /**
     * Auto Sync Configuration Constants
     */
    object AutoSync {
        const val CHECK_INTERVAL_MS = 60 * 1000L // 1 minute

        // Intervals
        const val INTERVAL_NONE = 0L
        const val INTERVAL_5_MIN = 5L * 60 * 1000
        const val INTERVAL_30_MIN = 30L * 60 * 1000
        const val INTERVAL_60_MIN = 60L * 60 * 1000
        const val INTERVAL_2_HOUR = 2L * 60 * 60 * 1000
        const val INTERVAL_6_HOUR = 6L * 60 * 60 * 1000
        const val INTERVAL_12_HOUR = 12L * 60 * 60 * 1000

        // Network Types
        const val NETWORK_WIFI_MOBILE = 0
        const val NETWORK_WIFI_ONLY = 1
        const val NETWORK_MOBILE_ONLY = 2
    }

    /**
     * Network Constants
     */
    object Network {
        /** Timeout for Supabase operations (e.g., edge function calls, DB queries) */
        const val SUPABASE_REQUEST_TIMEOUT_MS = 60_000L

        /** Max records per batch to avoid HTTP timeouts on large uploads */
        const val UPLOAD_BATCH_SIZE = 500
    }

    /**
     * Notification Constants
     */
    object Notification {
        // Auto Sync Channel
        const val CHANNEL_ID_AUTO_SYNC = "auto_sync_channel"
        const val CHANNEL_NAME_AUTO_SYNC = "Auto Sync Notifications"
        const val ID_AUTO_SYNC_SUCCESS = 1001
        const val ID_AUTO_SYNC_FAILURE = 1002
    }

    /**
     * Sensor Identifiers
     */
    object SensorId {
        const val HEART_RATE = "WatchHeartRate"
        const val ACCELEROMETER = "WatchAccelerometer"
        const val EDA = "WatchEDA"
        const val PPG = "WatchPPG"
        const val SKIN_TEMPERATURE = "WatchSkinTemperature"
        const val LOCATION = "WatchLocation"
    }

    /**
     * Language Constants
     */
    object Language {
        const val ENGLISH = "en"
        const val KOREAN = "ko"
    }
}
