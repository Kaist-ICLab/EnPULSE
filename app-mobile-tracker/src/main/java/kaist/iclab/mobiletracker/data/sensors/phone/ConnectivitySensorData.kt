package kaist.iclab.mobiletracker.data.sensors.phone

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Supabase data class representing connectivity sensor data from the phone device.
 *
 * @property eventId Unique identifier for this event (UUID v4).
 * @property uuid Unique identifier for the connectivity sensor entry. Auto-generated when inserting into Supabase.
 * @property deviceType Type of device (0 = phone, 1 = watch).
 * @property timestamp Unix timestamp in milliseconds when the connectivity status was recorded.
 * @property isConnected Whether the device is connected to a network.
 * @property hasInternet Whether the connection has internet capability.
 * @property networkType Primary connection type (e.g., WIFI, CELLULAR, ETHERNET, BLUETOOTH, VPN).
 * @property transportTypes List of transports available on the connection (e.g., ["WIFI", "VPN"]).
 * @property received Timestamp when the data was received by the phone (Unix timestamp in milliseconds).
 */
@Serializable
data class ConnectivitySensorData(
    @SerialName("event_id")
    val eventId: String,
    val uuid: String? = null,
    val timestamp: String,
    val received: String,
    @SerialName("device_type")
    val deviceType: Int,
    @SerialName("is_connected")
    val isConnected: Boolean,
    @SerialName("has_internet")
    val hasInternet: Boolean,
    @SerialName("network_type")
    val networkType: String,
    @SerialName("transport_types")
    val transportTypes: List<String>
)
