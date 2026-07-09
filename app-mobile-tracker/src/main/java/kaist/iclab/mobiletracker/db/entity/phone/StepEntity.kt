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
class StepEntity : BaseEntity, CsvSerializable {
    @SerialName("start_time")
    var startTime: Long = 0

    @SerialName("end_time")
    var endTime: Long = 0

    var steps: Long = 0

    constructor() : super()

    constructor(
        id: Long = 0,
        eventId: String = UUID.randomUUID().toString(),
        uuid: String = "",
        received: Long = 0,
        timestamp: Long = 0,
        deviceType: Int = DeviceType.PHONE.value,
        startTime: Long = 0,
        endTime: Long = 0,
        steps: Long = 0
    ) {
        initBaseEntity(id, eventId, uuid, received, timestamp, deviceType)
        this.startTime = startTime
        this.endTime = endTime
        this.steps = steps
    }

    override val csvHeader = "eventId,uuid,received,timestamp,startTime,endTime,steps"
    override fun toCsvRow() = "$eventId,$uuid,$received,$timestamp,$startTime,$endTime,$steps"
}
