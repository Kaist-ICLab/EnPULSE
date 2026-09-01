package kaist.iclab.mobiletracker.ui.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.AppRegistration
import androidx.compose.material.icons.filled.BackHand
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.TouchApp
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
        "ActivityRecognition" -> R.string.sensor_activity_recognition
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
        "Exercise" -> R.string.sensor_exercise
        "Sleep" -> R.string.sensor_sleep
        "Survey", "SurveySensor" -> R.string.sensor_survey
        "Accelerometer" -> R.string.sensor_accelerometer
        "EDA" -> R.string.sensor_eda
        "HeartRate" -> R.string.sensor_heart_rate
        "PPG" -> R.string.sensor_ppg
        "SkinTemperature" -> R.string.sensor_skin_temperature
        "ECG" -> R.string.sensor_ecg
        "IMU" -> R.string.sensor_imu
        "Gesture" -> R.string.sensor_gesture
        "Stress" -> R.string.sensor_stress
        "WebAppLog" -> R.string.sensor_web_app_log
        "VAD", "VadSensor" -> R.string.sensor_vad
        else -> R.string.sensor_desc_default
    }
}

/**
 * Get the string resource ID for the sensor description.
 */
fun getSensorDescriptionResId(sensorId: String): Int {
    val normalizedId = sensorId.replace(" ", "")
    return when (normalizedId) {
        "ActivityRecognition" -> R.string.sensor_desc_activity_recognition
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
        "Exercise" -> R.string.sensor_desc_exercise
        "Sleep" -> R.string.sensor_desc_sleep
        "Survey", "SurveySensor" -> R.string.sensor_desc_survey
        "ECG" -> R.string.sensor_desc_ecg
        "Accelerometer" -> R.string.sensor_desc_accelerometer
        "EDA" -> R.string.sensor_desc_eda
        "HeartRate" -> R.string.sensor_desc_heart_rate
        "PPG" -> R.string.sensor_desc_ppg
        "SkinTemperature" -> R.string.sensor_desc_skin_temperature
        "IMU" -> R.string.sensor_desc_imu
        "Gesture" -> R.string.sensor_desc_gesture
        "Stress" -> R.string.sensor_desc_stress
        "WebAppLog" -> R.string.sensor_desc_web_app_log
        "VAD", "VadSensor" -> R.string.sensor_desc_vad
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
        "Exercise" -> Icons.Default.FitnessCenter
        "Sleep" -> Icons.Default.Bedtime
        "Survey", "SurveySensor" -> Icons.AutoMirrored.Filled.Assignment
        "Accelerometer", "WatchAccelerometer" -> Icons.Default.Speed
        "EDA", "WatchEDA" -> Icons.Default.Waves
        "HeartRate", "WatchHeartRate" -> Icons.Default.FavoriteBorder
        "PPG", "WatchPPG" -> Icons.Default.MonitorHeart
        "SkinTemperature", "WatchSkinTemperature" -> Icons.Default.Thermostat
        "ECG" -> Icons.Default.Favorite
        "IMU", "WatchIMU" -> Icons.Default.CompassCalibration
        "Gesture", "WatchGesture" -> Icons.Default.BackHand
        "Stress", "WatchStress" -> Icons.Default.Psychology
        "VAD", "VadSensor" -> Icons.Default.RecordVoiceOver
        else -> Icons.Default.DataUsage
    }
}
