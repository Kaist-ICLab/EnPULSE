package kaist.iclab.mobiletracker.data.sensors.phone

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Supabase data class representing device mode sensor data from the phone device.
 *
 * @property eventId Unique identifier for this event (UUID v4).
 * @property uuid User UUID
 * @property timestamp Unix timestamp in milliseconds when the device mode event was recorded.
 * @property received Timestamp when the data was received by the phone (Unix timestamp in milliseconds).
 * @property deviceType Type of device (0 = phone, 1 = watch).
 * @property eventType Type of device mode event (e.g., "NOTIFICATION_MODE_EVENT", "POWER_SAVE_MODE_EVENT", "AIRPLANE_MODE_EVENT").
 * @property value Value of the device mode event (e.g., "NOTIFICATION_MODE_FILTER_ALL", "POWER_SAVE_MODE_ON", "AIRPLANE_MODE_OFF").
 */
@Serializable
data class DeviceModeSensorData(
    @SerialName("event_id")
    val eventId: String,
    val uuid: String? = null,
    val timestamp: String,
    val received: String,
    @SerialName("device_type")
    val deviceType: Int,
    @SerialName("event_type")
    val eventType: String,
    val value: String
)
