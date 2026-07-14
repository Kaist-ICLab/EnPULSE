package kaist.iclab.mobiletracker.data.sensors.phone

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ActivityRecognitionSensorData(
    @SerialName("event_id")
    val eventId: String,
    val uuid: String? = null,
    @SerialName("device_type")
    val deviceType: Int,
    val timestamp: String,
    val received: String,
    @SerialName("elapsed_realtime_millis")
    val elapsedRealtimeMillis: Long,
    @SerialName("activity_type")
    val activityType: Int,
    val score: Int,
    val probabilities: List<Int>
)
