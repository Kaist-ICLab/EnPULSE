package kaist.iclab.mobiletracker.ui.screens.SettingsScreen.PhoneSensorConfigSettings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.ui.utils.getSensorIcon
import kaist.iclab.mobiletracker.ui.utils.getSensorDescriptionResId
import kaist.iclab.mobiletracker.ui.utils.getSensorDisplayName

/**
 * Data class representing sensor configuration
 */
private data class SensorConfig(
    val patterns: List<String>,
    val canonicalId: String
)

/**
 * Sensor configurations mapping patterns to canonical IDs
 */
private val sensorConfigs = listOf(
    SensorConfig(listOf("Ambient Light"), "AmbientLight"),
    SensorConfig(listOf("App List Change"), "AppListChange"),
    SensorConfig(listOf("App Usage"), "AppUsage"),
    SensorConfig(listOf("Battery"), "Battery"),
    SensorConfig(listOf("Bluetooth"), "Bluetooth"),
    SensorConfig(listOf("Call Log"), "CallLog"),
    SensorConfig(listOf("Data Traffic"), "DataTraffic"),
    SensorConfig(listOf("Device Mode"), "DeviceMode"),
    SensorConfig(listOf("Location"), "Location"),
    SensorConfig(listOf("Media"), "Media"),
    SensorConfig(listOf("Message"), "Message"),
    SensorConfig(listOf("Connectivity"), "Connectivity"),
    SensorConfig(listOf("Notification"), "Notification"),
    SensorConfig(listOf("Screen"), "Screen"),
    SensorConfig(listOf("Step"), "Step"),
    SensorConfig(listOf("User Interaction"), "UserInteraction"),
    SensorConfig(listOf("Wifi", "WiFi"), "Wifi")
)

private val defaultConfig = SensorConfig(
    patterns = emptyList(),
    canonicalId = "Default"
)

/**
 * Finds the matching sensor configuration for a given sensor name
 */
private fun findSensorConfig(sensorName: String): SensorConfig {
    return sensorConfigs.firstOrNull { config ->
        config.patterns.any { pattern ->
            sensorName.contains(pattern, ignoreCase = true)
        }
    } ?: defaultConfig
}

/**
 * Maps sensor names to their localized titles
 */
@Composable
fun getLocalizedSensorTitle(sensorName: String): String {
    val config = findSensorConfig(sensorName)
    if (config.canonicalId == "Default") return sensorName
    return getSensorDisplayName(config.canonicalId)
}

/**
 * Maps sensor names to their localized descriptions (2-4 words)
 */
@Composable
fun getSensorDescription(sensorName: String): String {
    val config = findSensorConfig(sensorName)
    if (config.canonicalId == "Default") return stringResource(R.string.sensor_desc_default)
    return stringResource(getSensorDescriptionResId(config.canonicalId))
}

/**
 * Maps sensor names to their corresponding Material Icons
 */
fun getSensorIcon(sensorName: String): ImageVector {
    val config = findSensorConfig(sensorName)
    if (config.canonicalId == "Default") return Icons.Default.Settings
    return getSensorIcon(config.canonicalId)
}

