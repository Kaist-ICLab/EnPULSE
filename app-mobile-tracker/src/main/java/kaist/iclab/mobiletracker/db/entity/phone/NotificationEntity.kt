package kaist.iclab.mobiletracker.db.entity.phone

import io.objectbox.annotation.Entity
import kaist.iclab.mobiletracker.data.DeviceType
import kaist.iclab.mobiletracker.db.entity.BaseEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Entity
@Serializable
class NotificationEntity : BaseEntity {
    @SerialName("package_name")
    var packageName: String = ""

    @SerialName("event_type")
    var eventType: String = ""

    var title: String = ""
    var text: String = ""
    var visibility: Int = 0
    var category: String = ""

    constructor() : super()

    constructor(
        id: Long = 0,
        eventId: String = UUID.randomUUID().toString(),
        uuid: String = "",
        received: Long = 0,
        timestamp: Long = 0,
        deviceType: Int = DeviceType.PHONE.value,
        packageName: String = "",
        eventType: String = "",
        title: String = "",
        text: String = "",
        visibility: Int = 0,
        category: String = ""
    ) {
        initBaseEntity(id, eventId, uuid, received, timestamp, deviceType)
        this.packageName = packageName
        this.eventType = eventType
        this.title = title
        this.text = text
        this.visibility = visibility
        this.category = category
    }
}
