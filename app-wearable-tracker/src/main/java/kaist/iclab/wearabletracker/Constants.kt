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
    }

    /**
     * Notification ID Constants
     */
    object NotificationId {
        const val UPLOAD_DATA_SUCCESS = 1001
        const val UPLOAD_DATA_FAILURE = 1002
        const val FLUSH_DATA_SUCCESS = 1003
        const val FLUSH_DATA_FAILURE = 1004
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
    }
}

