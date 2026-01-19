package kaist.iclab.mobiletracker.data.sensors.phone

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Supabase data class representing call log sensor data from the phone device.
 *
 * @property eventId Unique identifier for this event (UUID v4).
 * @property uuid User UUID
 * @property timestamp Unix timestamp in milliseconds when the call was recorded.
 * @property received Timestamp when the data was received by the phone (Unix timestamp in milliseconds).
 * @property deviceType Type of device (0 = phone, 1 = watch).
 * @property duration Duration of the call in milliseconds.
 * @property number Phone number associated with the call.
 * @property type Type of call (e.g., CallLog.Calls.INCOMING_TYPE, OUTGOING_TYPE, MISSED_TYPE).
 */
@Serializable
data class CallLogSensorData(
    @SerialName("event_id")
    val eventId: String,
    val uuid: String? = null,
    val timestamp: String,
    val received: String,
    val duration: Long,
    val number: String,
    val type: Int,
    @SerialName("device_type")
    val deviceType: Int
)
