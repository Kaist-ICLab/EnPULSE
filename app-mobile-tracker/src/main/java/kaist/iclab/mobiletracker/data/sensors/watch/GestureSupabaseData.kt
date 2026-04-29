package kaist.iclab.mobiletracker.data.sensors.watch

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Gesture data model for Supabase upload.
 *
 * Probabilities are uploaded as the full array to preserve all class confidence scores.
 * The Supabase `sensor_gesture.probabilities` column should be of type `jsonb` or `integer[]`.
 *
 * See also [GestureSensorData] which is the BLE transport model (parsed from CSV).
 */
@Serializable
data class GestureSupabaseData(
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
