package kaist.iclab.mobiletracker.data.sensors.watch

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Supabase data class representing EDA (Electrodermal Activity) sensor data from the wearable device.
 *
 * @property eventId Unique identifier for this event (UUID v4).
 * @property uuid Unique identifier for the EDA entry. Auto-generated when inserting into Supabase.
 * @property deviceType Type of device (0 = phone, 1 = watch).
 * @property timestamp Unix timestamp in milliseconds when the EDA data was recorded.
 * @property skinConductance Skin conductance value in microsiemens (μS).
 * @property status Status of the EDA measurement.
 * @property received Timestamp when the data was received by the watch (Unix timestamp in milliseconds).
 */
@Serializable
data class EDASensorData(
    @SerialName("event_id")
    val eventId: String,
    val uuid: String? = null,
    val timestamp: String,
    val skinConductance: Float,
    val status: Int,
    val received: String,
    @SerialName("device_type")
    val deviceType: Int
)
