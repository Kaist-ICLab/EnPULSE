package kaist.iclab.mobiletracker.db.entity.phone

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index
import kaist.iclab.mobiletracker.data.DeviceType
import kaist.iclab.mobiletracker.db.obx.EpochMillisIsoSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.UUID

@Entity
@Serializable
data class MediaEntity(
    @Id
    @Transient
    var id: Long = 0,
    @SerialName("event_id")
    var eventId: String = UUID.randomUUID().toString(),
    var uuid: String = "",
    @Serializable(with = EpochMillisIsoSerializer::class)
    var received: Long = 0,
    @Index
    @Serializable(with = EpochMillisIsoSerializer::class)
    var timestamp: Long = 0,
    @SerialName("device_type")
    var deviceType: Int = DeviceType.PHONE.value,
    var operation: String = "",
    @SerialName("media_type")
    var mediaType: String = "",
    @SerialName("storage_type")
    var storageType: String = "",
    var uri: String = "",
    @SerialName("file_name")
    var fileName: String? = null,
    @SerialName("mime_type")
    var mimeType: String? = null,
    var size: Long? = null,
    @SerialName("date_added")
    @Serializable(with = EpochMillisIsoSerializer::class)
    var dateAdded: Long? = null,
    @SerialName("date_modified")
    @Serializable(with = EpochMillisIsoSerializer::class)
    var dateModified: Long? = null
)
