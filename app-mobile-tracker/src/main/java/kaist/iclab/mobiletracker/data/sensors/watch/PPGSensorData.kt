package kaist.iclab.mobiletracker.data.sensors.watch

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Supabase data class representing PPG (Photoplethysmography) sensor data from the wearable device.
 *
 * @property eventId Unique identifier for this event (UUID v4).
 * @property uuid Unique identifier for the PPG entry. Auto-generated when inserting into Supabase.
 * @property deviceType Type of device (0 = phone, 1 = watch).
 * @property timestamp Unix timestamp in milliseconds when the PPG data was recorded.
 * @property green Green light intensity value.
 * @property greenStatus Status of the green light measurement.
 * @property red Red light intensity value.
 * @property redStatus Status of the red light measurement.
 * @property ir Infrared light intensity value.
 * @property irStatus Status of the infrared light measurement.
 * @property received Timestamp when the data was received by the watch (Unix timestamp in milliseconds).
 */
@Serializable
data class PPGSensorData(
    @SerialName("event_id")
    val eventId: String,
    val uuid: String? = null,
    val timestamp: String,
    val green: Int,
    @SerialName("green_status")
    val greenStatus: Int,
    val red: Int,
    @SerialName("red_status")
    val redStatus: Int,
    val ir: Int,
    @SerialName("ir_status")
    val irStatus: Int,
    val received: String,
    @SerialName("device_type")
    val deviceType: Int
)
