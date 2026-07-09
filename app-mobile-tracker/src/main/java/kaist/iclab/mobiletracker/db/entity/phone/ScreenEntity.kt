package kaist.iclab.mobiletracker.db.entity.phone

import io.objectbox.annotation.Entity
import kaist.iclab.mobiletracker.data.DeviceType
import kaist.iclab.mobiletracker.db.entity.BaseEntity
import kaist.iclab.mobiletracker.db.entity.CsvSerializable
import kaist.iclab.mobiletracker.db.entity.RecordSerializable
import kaist.iclab.mobiletracker.repository.SensorRecord
import kotlinx.serialization.Serializable
import java.util.UUID

@Entity
@Serializable
class ScreenEntity : BaseEntity, CsvSerializable, RecordSerializable {
    var type: String = ""

    constructor() : super()

    constructor(
        id: Long = 0,
        eventId: String = UUID.randomUUID().toString(),
        uuid: String = "",
        received: Long = 0,
        timestamp: Long = 0,
        deviceType: Int = DeviceType.PHONE.value,
        type: String = ""
    ) {
        initBaseEntity(id, eventId, uuid, received, timestamp, deviceType)
        this.type = type
    }

    override val csvHeader = "eventId,uuid,received,timestamp,type"
    override fun toCsvRow() = "$eventId,$uuid,$received,$timestamp,$type"

    override fun toRecord() = SensorRecord(id = id, timestamp = timestamp, fields = mapOf("Type" to type))
}
