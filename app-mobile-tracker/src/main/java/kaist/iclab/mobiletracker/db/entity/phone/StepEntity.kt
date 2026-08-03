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
class StepEntity : BaseEntity, CsvSerializable, RecordSerializable {
    var duration: Long = 0

    var steps: Long = 0

    constructor() : super()

    constructor(
        id: Long = 0,
        eventId: String = UUID.randomUUID().toString(),
        uuid: String = "",
        received: Long = 0,
        timestamp: Long = 0,
        deviceType: Int = DeviceType.PHONE.value,
        duration: Long = 0,
        steps: Long = 0
    ) {
        initBaseEntity(id, eventId, uuid, received, timestamp, deviceType)
        this.duration = duration
        this.steps = steps
    }

    override fun csvHeader() = "eventId,uuid,received,timestamp,duration,steps"
    override fun toCsvRow() = "$eventId,$uuid,$received,$timestamp,$duration,$steps"

    override fun toRecord() = SensorRecord(id = id, timestamp = timestamp, fields = mapOf("Steps" to steps.toString()))
}
