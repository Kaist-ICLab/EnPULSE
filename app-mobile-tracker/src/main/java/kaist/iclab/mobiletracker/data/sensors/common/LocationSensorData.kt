package kaist.iclab.mobiletracker.data.sensors.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Supabase data class representing location sensor data from phone or watch devices.
 *
 * @property eventId Unique identifier for this event (UUID v4).
 * @property uuid Unique identifier for the location entry. Auto-generated when inserting into Supabase.
 * @property deviceType Type of device (use DeviceType.PHONE.value for phone, DeviceType.WATCH.value for watch).
 * @property timestamp Unix timestamp in milliseconds when the location was recorded.
 * @property accuracy Location accuracy in meters.
 * @property altitude Altitude in meters above sea level.
 * @property latitude Latitude coordinate in degrees.
 * @property longitude Longitude coordinate in degrees.
 * @property speed Speed in meters per second.
 * @property received Timestamp when the data was received (Unix timestamp in milliseconds).
 */
@Serializable
data class LocationSensorData(
    @SerialName("event_id")
    val eventId: String,
    val uuid: String? = null,
    val timestamp: String,
    val received: String,
    val accuracy: Float,
    val altitude: Double,
    val latitude: Double,
    val longitude: Double,
    val speed: Float,   
    @SerialName("device_type")
    val deviceType: Int,
)