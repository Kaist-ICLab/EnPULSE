package kaist.iclab.mobiletracker.data.sensors.watch

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Supabase data class representing heart rate sensor data from the wearable device.
 *
 * @property eventId Unique identifier for this event (UUID v4).
 * @property uuid Unique identifier for the heart rate entry. Auto-generated when inserting into Supabase.
 * @property deviceType Type of device (0 = phone, 1 = watch).
 * @property timestamp Unix timestamp in milliseconds when the heart rate data was recorded.
 * @property hr Heart rate value in beats per minute.
 * @property hrStatus Status of the heart rate measurement.
 * @property ibi List of inter-beat intervals in milliseconds.
 * @property ibiStatus List of status values corresponding to each inter-beat interval.
 * @property received Timestamp when the data was received by the watch (Unix timestamp in milliseconds).
 */
@Serializable
data class HeartRateSensorData(
    @SerialName("event_id")
    val eventId: String,
    val uuid: String? = null,
    val timestamp: String,
    val received: String,
    val hr: Int,
    val ibi: List<Int>,
    @SerialName("hr_status")
    val hrStatus: Int,
    @SerialName("ibi_status")
    val ibiStatus: List<Int>,
    @SerialName("device_type")
    val deviceType: Int
)
