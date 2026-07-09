package kaist.iclab.mobiletracker.db.entity.phone

import io.objectbox.annotation.Entity
import kaist.iclab.mobiletracker.data.DeviceType
import kaist.iclab.mobiletracker.db.entity.BaseEntity
import kaist.iclab.mobiletracker.db.entity.CsvSerializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Entity
@Serializable
class UserInteractionEntity : BaseEntity, CsvSerializable {
    @SerialName("package_name")
    var packageName: String = ""

    @SerialName("class_name")
    var className: String = ""

    @SerialName("event_type")
    var eventType: Int = 0

    var text: String = ""

    constructor() : super()

    constructor(
        id: Long = 0,
        eventId: String = "",
        uuid: String = "",
        received: Long = 0,
        timestamp: Long = 0,
        deviceType: Int = DeviceType.PHONE.value,
        packageName: String = "",
        className: String = "",
        eventType: Int = 0,
        text: String = ""
    ) {
        initBaseEntity(id, eventId, uuid, received, timestamp, deviceType)
        this.packageName = packageName
        this.className = className
        this.eventType = eventType
        this.text = text
    }

    override val csvHeader = "eventId,uuid,received,timestamp,packageName,className,eventType,text"
    override fun toCsvRow(): String {
        val escapedText = text.replace("\"", "\"\"")
        return "$eventId,$uuid,$received,$timestamp,$packageName,$className,$eventType,\"$escapedText\""
    }
}
