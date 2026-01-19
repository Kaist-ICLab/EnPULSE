package kaist.iclab.mobiletracker.data.sensors.phone

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Supabase data class representing data traffic sensor data from the phone device.
 *
 * @property eventId Unique identifier for this event (UUID v4).
 * @property uuid UUID of the current logged in user.
 * @property deviceType Type of device (0 = phone, 1 = watch).
 * @property timestamp Unix timestamp in milliseconds when the data traffic was recorded.
 * @property totalRx Total received bytes (cumulative).
 * @property totalTx Total transmitted bytes (cumulative).
 * @property mobileRx Mobile received bytes (cumulative).
 * @property mobileTx Mobile transmitted bytes (cumulative).
 * @property received Timestamp when the data was received by the phone (Unix timestamp in milliseconds).
 */
@Serializable
data class DataTrafficSensorData(
    @SerialName("event_id")
    val eventId: String,
    val uuid: String? = null,
    val received: String,
    val timestamp: String,
    @SerialName("device_type")
    val deviceType: Int,
    @SerialName("total_rx")
    val totalRx: Long,
    @SerialName("total_tx")
    val totalTx: Long,
    @SerialName("mobile_rx")
    val mobileRx: Long,
    @SerialName("mobile_tx")
    val mobileTx: Long
)
