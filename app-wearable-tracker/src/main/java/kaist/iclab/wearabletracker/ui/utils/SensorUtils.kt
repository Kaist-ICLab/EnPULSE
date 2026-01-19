package kaist.iclab.wearabletracker.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import kaist.iclab.wearabletracker.R

/**
 * Get the string resource ID for the sensor title (short name).
 */
fun getSensorTitleResId(sensorId: String): Int {
    val normalizedId = sensorId.replace(" ", "")
    return when (normalizedId) {
        "Accelerometer" -> R.string.sensor_accelerometer
        "PPG" -> R.string.sensor_ppg
        "HeartRate" -> R.string.sensor_heart_rate
        "SkinTemperature" -> R.string.sensor_skin_temperature
        "EDA" -> R.string.sensor_eda
        "Location" -> R.string.sensor_location
        else -> R.string.sensor_default
    }
}

/**
 * Get the localized display name (title) for a sensor.
 */
@Composable
fun getSensorDisplayName(sensorId: String): String {
    return stringResource(getSensorTitleResId(sensorId))
}
