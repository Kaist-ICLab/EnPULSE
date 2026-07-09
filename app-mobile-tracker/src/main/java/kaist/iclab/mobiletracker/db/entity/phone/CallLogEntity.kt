package kaist.iclab.mobiletracker.db.entity.phone

import io.objectbox.annotation.Entity
import kaist.iclab.mobiletracker.data.DeviceType
import kaist.iclab.mobiletracker.db.entity.BaseEntity
import kaist.iclab.mobiletracker.db.entity.CsvSerializable
import kotlinx.serialization.Serializable
import java.util.UUID

@Entity
@Serializable
class CallLogEntity : BaseEntity, CsvSerializable {
    var duration: Long = 0
    var number: String = ""
    var type: Int = 0

    constructor() : super()

    constructor(
        id: Long = 0,
        eventId: String = UUID.randomUUID().toString(),
        uuid: String = "",
        received: Long = 0,
        timestamp: Long = 0,
        deviceType: Int = DeviceType.PHONE.value,
        duration: Long = 0,
        number: String = "",
        type: Int = 0
    ) {
        initBaseEntity(id, eventId, uuid, received, timestamp, deviceType)
        this.duration = duration
        this.number = number
        this.type = type
    }

    override val csvHeader = "eventId,uuid,received,timestamp,duration,number,type"
    override fun toCsvRow() = "$eventId,$uuid,$received,$timestamp,$duration,$number,$type"
}
