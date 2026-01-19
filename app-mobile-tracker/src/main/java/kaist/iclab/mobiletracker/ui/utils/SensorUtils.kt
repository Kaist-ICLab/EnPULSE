package kaist.iclab.mobiletracker.ui.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.AppRegistration
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import kaist.iclab.mobiletracker.R

/**
 * Get the string resource ID for the sensor title (short name).
 */
fun getSensorTitleResId(sensorId: String): Int {
    val normalizedId = sensorId.replace(" ", "")
    return when (normalizedId) {
        "AmbientLight" -> R.string.sensor_ambient_light
        "AppListChange" -> R.string.sensor_app_list_change
        "AppUsage", "AppUsageLog" -> R.string.sensor_app_usage
        "Battery" -> R.string.sensor_battery
        "BluetoothScan", "Bluetooth" -> R.string.sensor_bluetooth_scan
        "CallLog" -> R.string.sensor_call_log
        "Connectivity" -> R.string.sensor_connectivity
        "DataTraffic" -> R.string.sensor_data_traffic
        "DeviceMode" -> R.string.sensor_device_mode
        "Location" -> R.string.sensor_location
        "Media" -> R.string.sensor_media
        "MessageLog", "Message" -> R.string.sensor_message
        "Notification" -> R.string.sensor_notification
        "Screen" -> R.string.sensor_screen
        "Step" -> R.string.sensor_step
        "UserInteraction" -> R.string.sensor_user_interaction
        "WifiScan", "Wifi", "WiFi" -> R.string.sensor_wifi_scan
        "WatchAccelerometer" -> R.string.sensor_accelerometer
        "WatchEDA" -> R.string.sensor_eda
        "WatchHeartRate" -> R.string.sensor_heart_rate
        "WatchPPG" -> R.string.sensor_ppg
        "WatchSkinTemperature" -> R.string.sensor_skin_temperature
        else -> R.string.sensor_desc_default
    }
}

/**
 * Get the string resource ID for the sensor description.
 */
fun getSensorDescriptionResId(sensorId: String): Int {
    val normalizedId = sensorId.replace(" ", "")
    return when (normalizedId) {
        "AmbientLight" -> R.string.sensor_desc_ambient_light
        "AppListChange" -> R.string.sensor_desc_app_list_change
        "AppUsage", "AppUsageLog" -> R.string.sensor_desc_app_usage
        "Battery" -> R.string.sensor_desc_battery
        "BluetoothScan", "Bluetooth" -> R.string.sensor_desc_bluetooth
        "CallLog" -> R.string.sensor_desc_call_log
        "Connectivity" -> R.string.sensor_desc_connectivity
        "DataTraffic" -> R.string.sensor_desc_data_traffic
        "DeviceMode" -> R.string.sensor_desc_device_mode
        "Location" -> R.string.sensor_desc_location
        "Media" -> R.string.sensor_desc_media
        "MessageLog", "Message" -> R.string.sensor_desc_message
        "Notification" -> R.string.sensor_desc_notification
        "Screen" -> R.string.sensor_desc_screen
        "Step" -> R.string.sensor_desc_step
        "UserInteraction" -> R.string.sensor_desc_user_interaction
        "WifiScan", "Wifi", "WiFi" -> R.string.sensor_desc_wifi
        else -> R.string.sensor_desc_default
    }
}

/**
 * Get the localized display name (title) for a sensor.
 */
@Composable
fun getSensorDisplayName(sensorId: String): String {
    return stringResource(getSensorTitleResId(sensorId))
}

/**
 * Get the localized description for a sensor.
 */
@Composable
fun getLocalizedSensorDescription(sensorId: String): String {
    return stringResource(getSensorDescriptionResId(sensorId))
}

/**
 * Get the icon for a sensor.
 */
fun getSensorIcon(sensorId: String): ImageVector {
    // Normalize logic matching AppColors
    val normalizedId = sensorId.replace(" ", "")
    
    return when (normalizedId) {
        "AmbientLight" -> Icons.Default.LightMode
        "AppListChange" -> Icons.Default.AppRegistration
        "AppUsage", "AppUsageLog" -> Icons.Default.GridView
        "Battery" -> Icons.Default.BatteryChargingFull
        "BluetoothScan", "Bluetooth" -> Icons.Default.Bluetooth
        "CallLog" -> Icons.Default.Call
        "Connectivity" -> Icons.Default.Wifi
        "DataTraffic" -> Icons.Default.DataUsage
        "DeviceMode" -> Icons.Default.SettingsSuggest
        "Location" -> Icons.Default.Place
        "Media" -> Icons.Default.PlayCircleOutline
        "MessageLog", "Message" -> Icons.AutoMirrored.Filled.Message
        "Notification" -> Icons.Default.Notifications
        "Screen" -> Icons.Default.StayCurrentPortrait
        "Step" -> Icons.AutoMirrored.Filled.DirectionsWalk
        "UserInteraction" -> Icons.Default.TouchApp
        "WifiScan", "Wifi" -> Icons.Default.WifiTethering
        "WatchAccelerometer" -> Icons.Default.Speed
        "WatchEDA" -> Icons.Default.Waves
        "WatchHeartRate" -> Icons.Default.FavoriteBorder
        "WatchPPG" -> Icons.Default.MonitorHeart
        "WatchSkinTemperature" -> Icons.Default.Thermostat
        else -> Icons.Default.DataUsage
    }
}
