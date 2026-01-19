package kaist.iclab.mobiletracker.data.sensors.phone

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Supabase data class representing notification sensor data from the phone device.
 *
 * @property eventId Unique identifier for this event (UUID v4).
 * @property uuid User UUID.
 * @property timestamp Unix timestamp in milliseconds when the notification event occurred.
 * @property received Timestamp when the data was received by the phone (Unix timestamp in milliseconds).
 * @property deviceType Type of device (0 = phone, 1 = watch).
 * @property packageName Package name of the app that generated the notification.
 * @property eventType Type of notification event: "POSTED" or "REMOVED".
 * @property title Title text of the notification.
 * @property text Content text of the notification.
 * @property visibility Visibility setting of the notification (-1 if not available).
 * @property category Category of the notification (empty string if not available).
 */
@Serializable
data class NotificationSensorData(
    @SerialName("event_id")
    val eventId: String,
    val uuid: String? = null,
    val timestamp: String,
    val received: String,
    val title: String,
    val text: String,
    val visibility: Int,
    val category: String,
    @SerialName("device_type")
    val deviceType: Int,
    @SerialName("package_name")
    val packageName: String,
    @SerialName("event_type")
    val eventType: String
)
