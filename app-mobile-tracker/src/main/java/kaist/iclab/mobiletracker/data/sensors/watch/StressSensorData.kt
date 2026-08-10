package kaist.iclab.mobiletracker.data.sensors.watch

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Stress sensor data model. Dual-purpose:
 * 1. BLE transport: parsed from CSV in [kaist.iclab.mobiletracker.utils.SensorDataCsvParser]
 * 2. Supabase upload: serialized via [kotlinx.serialization] with @SerialName for column mapping
 *
 * Note: @SerialName annotations map to Supabase column names. If the schema changes,
 * both the upload path and CSV parsing path must be reviewed.
 */
@Serializable
data class StressSensorData(
    @SerialName("event_id")
    val eventId: String,
    val uuid: String?,
    @SerialName("device_type")
    val deviceType: Int,
    val received: String,
    val timestamp: String,
    @SerialName("rmssd_1m")
    val rmssd1m: Float,
    @SerialName("ibi_count_1m")
    val ibiCount1m: Int,
    @SerialName("rmssd_5m")
    val rmssd5m: Float,
    @SerialName("ibi_count_5m")
    val ibiCount5m: Int,
    val threshold: Float,
    @SerialName("is_stressed")
    val isStressed: Boolean
)
