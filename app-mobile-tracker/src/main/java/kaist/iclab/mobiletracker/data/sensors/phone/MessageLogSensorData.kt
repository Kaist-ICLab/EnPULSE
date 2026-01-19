package kaist.iclab.mobiletracker.data.sensors.phone

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Supabase data class representing message log sensor data from the phone device.
 *
 * @property eventId Unique identifier for this event (UUID v4).
 * @property uuid User UUID.
 * @property timestamp Unix timestamp in milliseconds when the message was recorded.
 * @property received Timestamp when the data was received by the phone (Unix timestamp in milliseconds).
 * @property deviceType Type of device (0 = phone, 1 = watch).
 * @property number Phone number associated with the message.
 * @property messageType Type of message (e.g., "SMS", "MMS").
 * @property contactType Raw contact/message type from Android (e.g., Telephony.Sms.TYPE_* or Telephony.Mms.MESSAGE_BOX_*).
 */
@Serializable
data class MessageLogSensorData(
    @SerialName("event_id")
    val eventId: String,
    val uuid: String? = null,
    val timestamp: String,
    val received: String,
    @SerialName("device_type")
    val deviceType: Int,
    val number: String,
    @SerialName("message_type")
    val messageType: String,
    @SerialName("contact_type")
    val contactType: Int
)
