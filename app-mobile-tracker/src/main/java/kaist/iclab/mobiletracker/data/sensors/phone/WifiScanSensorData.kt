package kaist.iclab.mobiletracker.data.sensors.phone

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Supabase data class representing WiFi sensor data from the phone device.
 *
 * @property eventId Unique identifier for this event (UUID v4).
 * @property uuid UUID of the current logged in user.
 * @property deviceType Type of device (0 = phone, 1 = watch).
 * @property timestamp Unix timestamp in milliseconds when the WiFi data was recorded.
 * @property bssid Basic Service Set Identifier (MAC address) of the WiFi access point.
 * @property frequency WiFi frequency in MHz.
 * @property level Received Signal Strength Indicator in dBm.
 * @property ssid Service Set Identifier (network name) of the WiFi access point.
 * @property received Timestamp when the data was received by the phone (Unix timestamp in milliseconds).
 */
@Serializable
data class WifiScanSensorData(
    @SerialName("event_id")
    val eventId: String,
    val uuid: String? = null,
    val timestamp: String,
    val bssid: String,
    val frequency: Int,
    val level: Int,
    val ssid: String,
    val received: String,
    @SerialName("device_type")
    val deviceType: Int
)
