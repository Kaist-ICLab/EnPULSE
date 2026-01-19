package kaist.iclab.mobiletracker.data.sensors.phone

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Supabase data class representing app list change sensor data from the phone device.
 *
 * @property eventId Unique identifier for this event (UUID v4).
 * @property uuid User UUID
 * @property timestamp Unix timestamp in milliseconds when the app change was detected.
 * @property received Timestamp when the data was received by the phone (Unix timestamp in milliseconds).
 * @property deviceType Type of device (0 = phone, 1 = watch).
 * @property changedApp JSON object containing info about the specific app that changed (if any).
 *   Stored as native JSON/JSONB in Supabase (not stringified).
 * @property appList JSON array containing full snapshot of all apps (if included).
 *   Stored as native JSON/JSONB in Supabase (not stringified).
 */
@Serializable
data class AppListChangeSensorData(
    @SerialName("event_id")
    val eventId: String,
    val uuid: String? = null,
    val timestamp: String,
    val received: String,
    @SerialName("device_type")
    val deviceType: Int,
    @SerialName("changed_app")
    val changedApp: JsonElement? = null,
    @SerialName("app_list")
    val appList: JsonElement? = null
)
