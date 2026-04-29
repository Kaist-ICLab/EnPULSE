package kaist.iclab.mobiletracker.data.sensors.watch

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * IMU sensor data model. Dual-purpose:
 * 1. BLE transport: parsed from CSV in [kaist.iclab.mobiletracker.utils.SensorDataCsvParser]
 * 2. Supabase upload: serialized via [kotlinx.serialization] with @SerialName for column mapping
 *
 * Note: @SerialName annotations map to Supabase column names. If the schema changes,
 * both the upload path and CSV parsing path must be reviewed.
 */
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
