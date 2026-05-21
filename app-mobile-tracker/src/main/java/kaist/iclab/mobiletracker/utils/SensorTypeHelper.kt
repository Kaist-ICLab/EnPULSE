package kaist.iclab.mobiletracker.utils

/**
 * Utility class for identifying and working with phone and watch sensors.
 * Provides helper methods to determine sensor types and get sensor IDs.
 */
object SensorTypeHelper {
    /**
     * List of all watch sensor IDs (matching handler sensorId values)
     */
    val watchSensorIds: List<String> = listOf(
        "WatchHeartRate",
        "WatchAccelerometer",
        "WatchEDA",
        "WatchPPG",
        "WatchSkinTemperature",
        "WatchLocation",
        "WatchIMU",
        "WatchGesture",
        "WatchStress"
    )

    /**
     * Check if a sensor ID belongs to a watch sensor
     * @param sensorId The sensor ID to check
     * @return true if it's a watch sensor, false otherwise
     */
    fun isWatchSensor(sensorId: String): Boolean {
        return watchSensorIds.contains(sensorId)
    }

    /**
     * Check if a sensor ID belongs to a phone sensor
     * Note: This assumes any sensor ID that's not a watch sensor is a phone sensor.
     * For more accurate detection, you may need to check against the actual phone sensor list.
     * @param sensorId The sensor ID to check
     * @return true if it's likely a phone sensor, false otherwise
     */
    fun isPhoneSensor(sensorId: String): Boolean {
        return !isWatchSensor(sensorId)
    }
}

/**
 * Extension to convert library PascalCase sensor IDs to Supabase snake_case campaign names.
 * e.g., "AppUsageLog" -> "app_usage_log_sensor"
 */
fun String.toCampaignSensorName(): String {
    val baseName = if (this.startsWith("Watch")) {
        this.removePrefix("Watch")
    } else {
        this
    }
    return baseName
        .replace("([a-z])([A-Z]+)".toRegex(), "$1_$2")
        .replace("([A-Z]+)([A-Z][a-z])".toRegex(), "$1_$2")
        .lowercase() + "_sensor"
}
