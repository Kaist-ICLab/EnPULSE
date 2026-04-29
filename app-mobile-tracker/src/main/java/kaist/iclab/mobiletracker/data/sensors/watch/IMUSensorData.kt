package kaist.iclab.mobiletracker.data.sensors.watch

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IMUSensorData(
    @SerialName("event_id")
    val eventId: String,
    val uuid: String?,
    @SerialName("device_type")
    val deviceType: Int,
    val received: String,
    val timestamp: String,
    @SerialName("acc_x")
    val accX: Float,
    @SerialName("acc_y")
    val accY: Float,
    @SerialName("acc_z")
    val accZ: Float,
    @SerialName("gyro_x")
    val gyroX: Float,
    @SerialName("gyro_y")
    val gyroY: Float,
    @SerialName("gyro_z")
    val gyroZ: Float
)
