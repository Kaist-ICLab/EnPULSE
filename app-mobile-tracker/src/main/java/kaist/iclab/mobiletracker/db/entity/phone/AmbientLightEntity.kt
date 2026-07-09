package kaist.iclab.mobiletracker.db.entity.phone

import io.objectbox.annotation.Entity
import kaist.iclab.mobiletracker.data.DeviceType
import kaist.iclab.mobiletracker.db.entity.BaseEntity
import kaist.iclab.mobiletracker.db.entity.CsvSerializable
import kotlinx.serialization.Serializable
import java.util.UUID

@Entity
@Serializable
class AmbientLightEntity : BaseEntity, CsvSerializable {
    var accuracy: Int = 0
    var value: Float = 0f

    constructor() : super()

    constructor(
        id: Long = 0,
        eventId: String = UUID.randomUUID().toString(),
        uuid: String = "",
        received: Long = 0,
        timestamp: Long = 0,
        deviceType: Int = DeviceType.PHONE.value,
        accuracy: Int = 0,
        value: Float = 0f
    ) {
        initBaseEntity(id, eventId, uuid, received, timestamp, deviceType)
        this.accuracy = accuracy
        this.value = value
    }

    override val csvHeader = "eventId,uuid,received,timestamp,accuracy,value"
    override fun toCsvRow() = "$eventId,$uuid,$received,$timestamp,$accuracy,$value"
}
