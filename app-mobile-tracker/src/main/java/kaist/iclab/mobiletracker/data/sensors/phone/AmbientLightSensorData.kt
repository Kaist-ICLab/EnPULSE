package kaist.iclab.mobiletracker.data.sensors.phone

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Supabase data class representing ambient light sensor data from the phone device.
 *
 * @property eventId Unique identifier for this event (UUID v4).
 * @property uuid UUID of the current logged in user.
 * @property deviceType Type of device (0 = phone, 1 = watch).
 * @property timestamp Unix timestamp in milliseconds when the ambient light data was recorded.
 * @property value Ambient light value in lux (illuminance).
 * @property received Timestamp when the data was received by the phone (Unix timestamp in milliseconds).
 * @property accuracy Sensor accuracy (0 = SENSOR_STATUS_UNRELIABLE, 1 = SENSOR_STATUS_ACCURACY_LOW, 2 = SENSOR_STATUS_ACCURACY_MEDIUM, 3 = SENSOR_STATUS_ACCURACY_HIGH).
 */
@Serializable
data class AmbientLightSensorData(
    @SerialName("event_id")
    val eventId: String,
    val uuid: String? = null,
    val timestamp: String,
    val received: String,
    val value: Float,
    val accuracy: Int,
    @SerialName("device_type")
    val deviceType: Int
)
