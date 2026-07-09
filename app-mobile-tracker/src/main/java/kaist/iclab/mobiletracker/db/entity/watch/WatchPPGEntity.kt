package kaist.iclab.mobiletracker.db.entity.watch

import io.objectbox.annotation.Entity
import kaist.iclab.mobiletracker.data.DeviceType
import kaist.iclab.mobiletracker.db.entity.BaseEntity
import kaist.iclab.mobiletracker.db.entity.CsvSerializable
import kaist.iclab.mobiletracker.db.entity.RecordSerializable
import kaist.iclab.mobiletracker.repository.SensorRecord
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Entity
@Serializable
class WatchPPGEntity : BaseEntity, CsvSerializable, RecordSerializable {
    var green: Int = 0

    @SerialName("green_status")
    var greenStatus: Int = 0

    var red: Int = 0

    @SerialName("red_status")
    var redStatus: Int = 0

    var ir: Int = 0

    @SerialName("ir_status")
    var irStatus: Int = 0

    constructor() : super()

    constructor(
        id: Long = 0,
        eventId: String = "",
        uuid: String = "",
        received: Long = 0,
        timestamp: Long = 0,
        deviceType: Int = DeviceType.WATCH.value,
        green: Int = 0,
        greenStatus: Int = 0,
        red: Int = 0,
        redStatus: Int = 0,
        ir: Int = 0,
        irStatus: Int = 0
    ) {
        initBaseEntity(id, eventId, uuid, received, timestamp, deviceType)
        this.green = green
        this.greenStatus = greenStatus
        this.red = red
        this.redStatus = redStatus
        this.ir = ir
        this.irStatus = irStatus
    }

    override val csvHeader = "eventId,uuid,received,timestamp,green,greenStatus,red,redStatus,ir,irStatus"
    override fun toCsvRow() = "$eventId,$uuid,$received,$timestamp,$green,$greenStatus,$red,$redStatus,$ir,$irStatus"

    override fun toRecord() = SensorRecord(id = id, timestamp = timestamp, fields = mapOf("Green" to green.toString(), "IR" to ir.toString()))
}
