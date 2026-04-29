package kaist.iclab.mobiletracker.data.sensors.watch

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GestureSensorData(
    @SerialName("event_id")
    val eventId: String,
    val uuid: String?,
    @SerialName("device_type")
    val deviceType: Int,
    val received: String,
    val timestamp: String,
    @SerialName("class_index")
    val classIndex: Int,
    val score: Int,
    val probabilities: List<Int>
)
