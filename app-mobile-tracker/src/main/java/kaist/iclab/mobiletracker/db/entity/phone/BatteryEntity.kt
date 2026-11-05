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
class BatteryEntity : BaseEntity, CsvSerializable, RecordSerializable {
    @SerialName("connected_type")
    var connectedType: Int = 0
    var status: Int = 0
    var level: Int = 0
    var temperature: Int = 0

    constructor() : super()

    constructor(
        id: Long = 0,
        eventId: String = UUID.randomUUID().toString(),
        uuid: String = "",
        received: Long = 0,
        timestamp: Long = 0,
        deviceType: Int = DeviceType.PHONE.value,
        connectedType: Int = 0,
        status: Int = 0,
        level: Int = 0,
        temperature: Int = 0
    ) {
        initBaseEntity(id, eventId, uuid, received, timestamp, deviceType)
        this.connectedType = connectedType
        this.status = status
        this.level = level
        this.temperature = temperature
    }

    override fun csvHeader() =
        "eventId,uuid,received,timestamp,connectedType,status,level,temperature"

    override fun toCsvRow() =
        "$eventId,$uuid,$received,$timestamp,$connectedType,$status,$level,$temperature"

    override fun toRecord() = SensorRecord(
        id = id,
        timestamp = timestamp,
        fields = mapOf("Level" to "$level%", "Status" to status.toString())
    )
}
