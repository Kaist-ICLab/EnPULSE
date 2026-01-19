package kaist.iclab.mobiletracker.data.sensors.phone

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Supabase data class representing battery sensor data from the phone device.
 *
 * @property eventId Unique identifier for this event (UUID v4).
 * @property uuid UUID of the current logged in user.
 * @property deviceType Type of device (0 = phone, 1 = watch).
 * @property timestamp Unix timestamp in milliseconds when the battery data was recorded.
 * @property level Battery level as a percentage (0 to 100).
 * @property connectedType Charging connection type (e.g., BatteryManager.BATTERY_PLUGGED_AC, BATTERY_PLUGGED_USB, BATTERY_PLUGGED_WIRELESS).
 * @property status Battery status (e.g., BatteryManager.BATTERY_STATUS_CHARGING, BATTERY_STATUS_DISCHARGING, BATTERY_STATUS_FULL, BATTERY_STATUS_NOT_CHARGING).
 * @property temperature Battery temperature in degrees Celsius.
 * @property received Timestamp when the data was received by the phone (Unix timestamp in milliseconds).
 */
@Serializable
data class BatterySensorData(
    @SerialName("event_id")
    val eventId: String,
    val uuid: String? = null,
    val timestamp: String,
    val level: Int,
    val status: Int,
    val temperature: Int,
    val received: String,
    @SerialName("device_type")
    val deviceType: Int,
    @SerialName("connected_type")
    val connectedType: Int
)
