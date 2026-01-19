package kaist.iclab.mobiletracker.data.sensors.phone

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Supabase data class representing Bluetooth scan sensor data from the phone device.
 *
 * @property eventId Unique identifier for this event (UUID v4).
 * @property uuid UUID of the current logged in user.
 * @property deviceType Type of device (0 = phone, 1 = watch).
 * @property timestamp Unix timestamp in milliseconds when the Bluetooth scan data was recorded.
 * @property name Name of the Bluetooth device.
 * @property alias Alias of the Bluetooth device.
 * @property address MAC address of the Bluetooth device.
 * @property bondState Bond state of the device (e.g., 10 = BOND_NONE, 11 = BOND_BONDING, 12 = BOND_BONDED).
 * @property connectionType Connection type (e.g., 1 = DEVICE_TYPE_CLASSIC, 2 = DEVICE_TYPE_LE, 3 = DEVICE_TYPE_DUAL).
 * @property classType Bluetooth class type (device class code).
 * @property rssi Received Signal Strength Indicator in dBm.
 * @property isLE Whether the device is a Low Energy (LE) device.
 * @property received Timestamp when the data was received by the phone (Unix timestamp in milliseconds).
 */
@Serializable
data class BluetoothScanSensorData(
    @SerialName("event_id")
    val eventId: String,
    val uuid: String? = null,
    val timestamp: String,
    val name: String,
    val alias: String,
    val address: String,
    val rssi: Int,
    val received: String,
    @SerialName("device_type")
    val deviceType: Int,
    @SerialName("bond_state")
    val bondState: Int,
    @SerialName("connection_type")
    val connectionType: Int,
    @SerialName("class_type")
    val classType: Int,
    @SerialName("is_le")
    val isLE: Boolean
)
