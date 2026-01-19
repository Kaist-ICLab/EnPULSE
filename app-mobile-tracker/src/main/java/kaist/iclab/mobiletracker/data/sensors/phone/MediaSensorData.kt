package kaist.iclab.mobiletracker.data.sensors.phone

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Supabase data class representing media sensor data from the phone device.
 *
 * @property eventId Unique identifier for this event (UUID v4).
 * @property uuid Unique identifier for the media sensor entry. Auto-generated when inserting into Supabase.
 * @property timestamp Unix timestamp in milliseconds when the media data was recorded.
 * @property received Timestamp when the data was received by the phone (Unix timestamp in milliseconds).
 * @property deviceType Type of device (0 = phone, 1 = watch).
 * @property operation Type of operation: "CREATE", "UPDATE", "DELETE".
 * @property mediaType Type of media: "IMAGE", "VIDEO", "AUDIO", "DOCUMENT".
 * @property storageType Type of storage: "INTERNAL", "EXTERNAL".
 * @property uri URI of the media file.
 * @property fileName Name of the media file (optional).
 * @property mimeType MIME type of the media file (optional, e.g., "image/jpeg", "video/mp4").
 * @property size Size of the media file in bytes (optional).
 * @property dateAdded Timestamp when the file was added (optional, Unix timestamp in milliseconds).
 * @property dateModified Timestamp when the file was last modified (optional, Unix timestamp in milliseconds).
 */
@Serializable
data class MediaSensorData(
    @SerialName("event_id")
    val eventId: String,
    val uuid: String? = null,
    val timestamp: String,
    val received: String,
    @SerialName("device_type")
    val deviceType: Int,
    val operation: String,
    @SerialName("media_type")
    val mediaType: String,
    @SerialName("storage_type")
    val storageType: String,
    val uri: String,
    @SerialName("file_name")
    val fileName: String? = null,
    @SerialName("mime_type")
    val mimeType: String? = null,
    val size: Long? = null,
    @SerialName("date_added")
    val dateAdded: String? = null,
    @SerialName("date_modified")
    val dateModified: String? = null
)
