package kaist.iclab.mobiletracker.data.sensors.watch

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Gesture sensor data model for BLE transport (CSV parsing from watch).
 * Parsed by [kaist.iclab.mobiletracker.utils.SensorDataCsvParser.parseGestureCsv].
 *
 * Note: This class retains the full probabilities array (List<Int>).
 * For Supabase upload, see [GestureSupabaseData] which reduces probabilities to a single Int.
 */
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
