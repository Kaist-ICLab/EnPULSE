package kaist.iclab.wearabletracker

/**
 * Centralized constants for the wearable tracker app.
 * All constants used across the app should be defined here.
 */
object Constants {
    /**
     * BLE Communication Constants
     */
    object BLE {
        const val KEY_SENSOR_DATA = "sensor_data_csv"
        const val KEY_SYNC_ACK = "sync_ack"
        const val KEY_MICRO_EMA_RESPONSE = "micro_ema_response"
        const val KEY_MICRO_EMA_ACK = "micro_ema_ack"
        const val KEY_MICRO_EMA_TRIGGER = "micro_ema_trigger"
        const val KEY_TRIGGER_CONFIG = "trigger_config"
        const val KEY_PHONE_EMA_TRIGGER = "phone_ema_trigger"
        const val KEY_DETECTION_STATE_UPDATE = "detection_state_update"
        const val KEY_ACTIVE_SENSOR_CONFIG = "active_sensor_config"
        const val KEY_WEBAPP_TRIGGER = "webapp_trigger"
        const val KEY_NOTIFICATION_TRIGGER = "notification_trigger"
        const val KEY_BROADCAST_TRIGGER = "broadcast_trigger"
    }

    /**
     * Trigger action identifiers recognized by [kaist.iclab.wearabletracker.trigger.WatchTriggerActionHandler]
     * beyond the generic [kaist.iclab.tracker.trigger.model.TriggerActionConfig.Broadcast] passthrough.
     */
    object Trigger {
        /**
         * Broadcast action id used by the Dashboard's "open webapp" trigger preset. The watch is
         * the only place trigger conditions are evaluated (see Step 0 of the EnPULSE WebView
         * platform plan), so a local `context.sendBroadcast()` here would have no listener — this
         * action is recognized and forwarded to the phone over BLE instead.
         */
        const val ACTION_OPEN_WEBAPP = "kaist.iclab.mobiletracker.OPEN_WEBAPP"
    }

    /**
     * Sensor Type Constants
     */
    object SensorType {
        const val ACCELEROMETER = "Accelerometer"
        const val PPG = "PPG"
        const val HEART_RATE = "HeartRate"
        const val SKIN_TEMPERATURE = "SkinTemperature"
        const val EDA = "EDA"
        const val LOCATION = "Location"
    }

    /**
     * Notification Channel Constants
     */
    object NotificationChannel {
        const val UPLOAD_DATA_ID = "upload_data_channel"
        const val UPLOAD_DATA_NAME = "Upload Data"
        const val UPLOAD_DATA_DESCRIPTION = "Notifications for data upload status"

        const val FLUSH_DATA_ID = "flush_data_channel"
        const val FLUSH_DATA_NAME = "Flush Data"
        const val FLUSH_DATA_DESCRIPTION = "Notifications for data flush status"

        const val ERROR_ID = "error_channel"
        const val ERROR_NAME = "Errors"
        const val ERROR_DESCRIPTION = "Notifications for application errors and exceptions"

        const val TRIGGER_ID = "trigger_channel"
        const val TRIGGER_NAME = "Survey Triggers"
        const val TRIGGER_DESCRIPTION = "High-priority notifications for microEMA surveys"
    }

    /**
     * Notification ID Constants
     */
    object NotificationId {
        const val UPLOAD_DATA_SUCCESS = 1001
        const val UPLOAD_DATA_FAILURE = 1002
        const val FLUSH_DATA_SUCCESS = 1003
        const val FLUSH_DATA_FAILURE = 1004
        const val TRIGGER = 1005
        const val ERROR = 2000 // Base ID for errors, will be incremented for multiple errors
    }

    /**
     * Device Info Constants
     */
    object DeviceInfo {
        const val UNKNOWN_NAME = "Unknown"
        const val UNKNOWN_ID = "Unknown"
    }

    /**
     * Database Constants
     */
    object DB {
        const val BUFFER_SIZE = 1000
        const val BATCH_SIZE = 50
        const val FLUSH_INTERVAL_MS = 2000L
        const val SYNC_BATCH_LIMIT = 2000 // Max records per sync batch
        const val MIN_FREE_SPACE_BYTES = 100 * 1024 * 1024L // 100MB
    }

    /**
     * AutoSync Constants
     */
    object AutoSync {
        const val WAKELOCK_TAG = "EnPulse:AutoSyncWakeLock"
        const val WAKELOCK_TIMEOUT_MS = 5 * 60 * 1000L // 5 minutes
        const val MIN_SYNC_INTERVAL_MS = 60 * 1000L // Minimum 1 minute between syncs
    }
}

