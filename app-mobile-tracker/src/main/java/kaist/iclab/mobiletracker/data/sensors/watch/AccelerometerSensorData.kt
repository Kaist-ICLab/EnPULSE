package kaist.iclab.mobiletracker.data.sensors.watch

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Supabase data class representing accelerometer sensor data from the wearable device.
 *
 * @property eventId Unique identifier for this event (UUID v4).
 * @property uuid Unique identifier for the accelerometer entry. Auto-generated when inserting into Supabase.
 * @property deviceType Type of device (0 = phone, 1 = watch).
 * @property timestamp Unix timestamp in milliseconds when the accelerometer data was recorded.
 * @property x Acceleration along the x-axis in m/s².
 * @property y Acceleration along the y-axis in m/s².
 * @property z Acceleration along the z-axis in m/s².
 * @property received Timestamp when the data was received by the watch (Unix timestamp in milliseconds).
 */
@Serializable
data class AccelerometerSensorData(
    @SerialName("event_id")
    val eventId: String,
    val uuid: String? = null,
    val timestamp: String,
    val x: Float,
    val y: Float,
    val z: Float,
    val received: String,
    @SerialName("device_type")
    val deviceType: Int
)
