package kaist.iclab.mobiletracker.data.sensors.watch

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StressSensorData(
    @SerialName("event_id")
    val eventId: String,
    val uuid: String?,
    @SerialName("device_type")
    val deviceType: Int,
    val received: String,
    val timestamp: String,
    @SerialName("window_start")
    val windowStart: String,
    val probability: Float,
    @SerialName("is_high_stress")
    val isHighStress: Boolean
)
