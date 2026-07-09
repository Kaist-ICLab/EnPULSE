package kaist.iclab.mobiletracker.db.entity.phone

import io.objectbox.annotation.Entity
import kaist.iclab.mobiletracker.data.DeviceType
import kaist.iclab.mobiletracker.db.entity.BaseEntity
import kaist.iclab.mobiletracker.db.entity.CsvSerializable
import kaist.iclab.mobiletracker.db.entity.RecordSerializable
import kaist.iclab.mobiletracker.repository.SensorRecord
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Entity
@Serializable
class MessageLogEntity : BaseEntity, CsvSerializable, RecordSerializable {
    var number: String = ""

    @SerialName("message_type")
    var messageType: String = ""

    @SerialName("contact_type")
    var contactType: Int = 0

    constructor() : super()

    constructor(
        id: Long = 0,
        eventId: String = UUID.randomUUID().toString(),
        uuid: String = "",
        received: Long = 0,
        timestamp: Long = 0,
        deviceType: Int = DeviceType.PHONE.value,
        number: String = "",
        messageType: String = "",
        contactType: Int = 0
    ) {
        initBaseEntity(id, eventId, uuid, received, timestamp, deviceType)
        this.number = number
        this.messageType = messageType
        this.contactType = contactType
    }

    override val csvHeader = "eventId,uuid,received,timestamp,number,messageType,contactType"
    override fun toCsvRow() = "$eventId,$uuid,$received,$timestamp,$number,$messageType,$contactType"

    override fun toRecord() = SensorRecord(id = id, timestamp = timestamp, fields = mapOf("Type" to messageType, "Number" to number.take(10)))
}
