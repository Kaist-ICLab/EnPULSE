package kaist.iclab.mobiletracker.data.sensors.phone

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Supabase data class representing step sensor data from the phone device.
 *
 * @property eventId Unique identifier for this event (UUID v4).
 * @property uuid User UUID.
 * @property timestamp Unix timestamp in milliseconds when the step bucket was recorded.
 * @property received Timestamp when the data was received by the phone (Unix timestamp in milliseconds).
 * @property deviceType Type of device (0 = phone, 1 = watch).
 * @property startTime Start time of the step bucket (Unix timestamp in milliseconds).
 * @property endTime End time of the step bucket (Unix timestamp in milliseconds).
 * @property steps Number of steps in this bucket.
 */
@Serializable
data class StepSensorData(
    @SerialName("event_id")
    val eventId: String,
    val uuid: String? = null,
    val timestamp: String,
    val received: String,
    val steps: Long,
    @SerialName("device_type")
    val deviceType: Int,
    @SerialName("start_time")
    val startTime: Long,
    @SerialName("end_time")
    val endTime: Long
)
