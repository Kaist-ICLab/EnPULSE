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

        /**
         * ObjectBox's own default cap (`BoxStoreBuilder.DEFAULT_MAX_DB_SIZE_KBYTE`) is 1 GB —
         * writes past it throw `DbFullException`. Since this app intentionally keeps sensor data
         * locally forever (pruning is disabled, see SensorUploadService.kt), 1 GB is nowhere near
         * enough headroom for continuous multi-sensor collection over a real study's duration.
         * Raised generously here; still a hard cap, not "unlimited", to guard against runaway/
         * corrupted growth silently filling the device's storage.
         */
        const val OBJECTBOX_MAX_SIZE_KB = 32L * 1024 * 1024 // 32 GB

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
        const val CHECK_INTERVAL_MS = 60 * 1000L // 1 minute — WebAppLogSyncWorker's fixed cadence

        // WorkManager unique work names (see SensorAutoSyncWorker / WebAppLogSyncWorker). Both are
        // self-rescheduling OneTimeWorkRequests rather than PeriodicWorkRequests, since the
        // configurable sync interval (5 min option below) is under WorkManager's 15-minute
        // PeriodicWorkRequest floor.
        const val WORK_NAME_SENSOR_SYNC = "sensor_auto_sync_work"
        const val WORK_NAME_WEBAPP_LOG_SYNC = "webapp_log_sync_work"

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
        // Data Upload Channel — foreground service used by BOTH "Upload Now" and auto-sync
        // (see kaist.iclab.mobiletracker.services.upload.DataUploadService)
        const val CHANNEL_ID_DATA_UPLOAD = "data_upload_channel"
        const val CHANNEL_NAME_DATA_UPLOAD = "Data Upload"
        const val ID_DATA_UPLOAD_PROGRESS = 1001
        const val ID_DATA_UPLOAD_RESULT = 1002

        // Survey Notifications
        const val ID_SURVEY_BASE = 2000
        const val CHANNEL_ID_SURVEY = "survey_channel"
        const val CHANNEL_NAME_SURVEY = "Survey Notifications"
        const val CHANNEL_ID_SURVEY_TRIGGER = "survey_trigger_channel"
        const val CHANNEL_NAME_SURVEY_TRIGGER = "Survey Triggers"
    }

    /**
     * Sensor Identifiers
     */
    object SensorId {
        const val HEART_RATE = "HeartRate"
        const val ACCELEROMETER = "Accelerometer"
        const val EDA = "EDA"
        const val PPG = "PPG"
        const val SKIN_TEMPERATURE = "SkinTemperature"
        const val LOCATION = "Location"
        const val ECG = "ECG"
        const val IMU = "IMU"
        const val GESTURE = "Gesture"
        const val STRESS = "Stress"
    }

    /**
     * Language Constants
     */
    object Language {
        const val ENGLISH = "en"
        const val KOREAN = "ko"
    }

    /**
     * Trigger Constants
     */
    object Trigger {
        /** Threshold for discarding stale BLE triggers (5 minutes) */
        const val STALE_THRESHOLD_MS = 5 * 60 * 1000L

        /**
         * Broadcast action id used by the Dashboard's "open webapp" trigger preset. Recognized by
         * [kaist.iclab.mobiletracker.trigger.PhoneTriggerActionHandler] and routed to
         * [kaist.iclab.mobiletracker.webapp.WebAppTriggerHandler.launch] directly instead of a
         * real `context.sendBroadcast()`, since no receiver is registered for it — this is a
         * dashboard authoring convention, not an actual Android broadcast contract. Must match
         * the watch's `Constants.Trigger.ACTION_OPEN_WEBAPP` string value exactly.
         */
        const val ACTION_OPEN_WEBAPP = "kaist.iclab.mobiletracker.OPEN_WEBAPP"
    }
}
