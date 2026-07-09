package kaist.iclab.mobiletracker.db.entity.phone

import io.objectbox.annotation.Entity
import kaist.iclab.mobiletracker.data.DeviceType
import kaist.iclab.mobiletracker.db.entity.BaseEntity
import kaist.iclab.mobiletracker.db.entity.CsvSerializable
import kaist.iclab.mobiletracker.db.obx.EpochMillisIsoSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Entity
@Serializable
class MediaEntity : BaseEntity, CsvSerializable {
    var operation: String = ""

    @SerialName("media_type")
    var mediaType: String = ""

    @SerialName("storage_type")
    var storageType: String = ""

    var uri: String = ""

    @SerialName("file_name")
    var fileName: String? = null

    @SerialName("mime_type")
    var mimeType: String? = null

    var size: Long? = null

    @SerialName("date_added")
    @Serializable(with = EpochMillisIsoSerializer::class)
    var dateAdded: Long? = null

    @SerialName("date_modified")
    @Serializable(with = EpochMillisIsoSerializer::class)
    var dateModified: Long? = null

    constructor() : super()

    constructor(
        id: Long = 0,
        eventId: String = UUID.randomUUID().toString(),
        uuid: String = "",
        received: Long = 0,
        timestamp: Long = 0,
        deviceType: Int = DeviceType.PHONE.value,
        operation: String = "",
        mediaType: String = "",
        storageType: String = "",
        uri: String = "",
        fileName: String? = null,
        mimeType: String? = null,
        size: Long? = null,
        dateAdded: Long? = null,
        dateModified: Long? = null
    ) {
        initBaseEntity(id, eventId, uuid, received, timestamp, deviceType)
        this.operation = operation
        this.mediaType = mediaType
        this.storageType = storageType
        this.uri = uri
        this.fileName = fileName
        this.mimeType = mimeType
        this.size = size
        this.dateAdded = dateAdded
        this.dateModified = dateModified
    }

    override val csvHeader = "eventId,uuid,received,timestamp,operation,mediaType,storageType,uri,fileName,mimeType,size,dateAdded,dateModified"
    override fun toCsvRow() = "$eventId,$uuid,$received,$timestamp,$operation,$mediaType,$storageType,$uri,$fileName,$mimeType,$size,$dateAdded,$dateModified"
}
