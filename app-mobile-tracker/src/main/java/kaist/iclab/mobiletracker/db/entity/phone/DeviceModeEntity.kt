package kaist.iclab.mobiletracker.db.entity.phone

import io.objectbox.annotation.Entity
import kaist.iclab.mobiletracker.data.DeviceType
import kaist.iclab.mobiletracker.db.entity.BaseEntity
import kaist.iclab.mobiletracker.db.entity.CsvSerializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Entity
@Serializable
class DeviceModeEntity : BaseEntity, CsvSerializable {
    @SerialName("event_type")
    var eventType: String = ""
    var value: String = ""

    constructor() : super()

    constructor(
        id: Long = 0,
        eventId: String = UUID.randomUUID().toString(),
        uuid: String = "",
        received: Long = 0,
        timestamp: Long = 0,
        deviceType: Int = DeviceType.PHONE.value,
        eventType: String = "",
        value: String = ""
    ) {
        initBaseEntity(id, eventId, uuid, received, timestamp, deviceType)
        this.eventType = eventType
        this.value = value
    }

    override val csvHeader = "eventId,uuid,received,timestamp,eventType,value"
    override fun toCsvRow() = "$eventId,$uuid,$received,$timestamp,$eventType,$value"
}
