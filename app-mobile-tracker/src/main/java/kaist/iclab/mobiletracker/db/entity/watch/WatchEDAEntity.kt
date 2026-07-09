package kaist.iclab.mobiletracker.db.entity.watch

import io.objectbox.annotation.Entity
import kaist.iclab.mobiletracker.data.DeviceType
import kaist.iclab.mobiletracker.db.entity.BaseEntity
import kaist.iclab.mobiletracker.db.entity.CsvSerializable
import kaist.iclab.mobiletracker.db.entity.RecordSerializable
import kaist.iclab.mobiletracker.repository.SensorRecord
import kotlinx.serialization.Serializable

@Entity
@Serializable
class WatchEDAEntity : BaseEntity, CsvSerializable, RecordSerializable {
    var skinConductance: Float = 0f
    var status: Int = 0

    constructor() : super()

    constructor(
        id: Long = 0,
        eventId: String = "",
        uuid: String = "",
        received: Long = 0,
        timestamp: Long = 0,
        deviceType: Int = DeviceType.WATCH.value,
        skinConductance: Float = 0f,
        status: Int = 0
    ) {
        initBaseEntity(id, eventId, uuid, received, timestamp, deviceType)
        this.skinConductance = skinConductance
        this.status = status
    }

    override fun csvHeader() = "eventId,uuid,received,timestamp,skinConductance,status"
    override fun toCsvRow() = "$eventId,$uuid,$received,$timestamp,$skinConductance,$status"

    override fun toRecord() = SensorRecord(id = id, timestamp = timestamp, fields = mapOf("EDA" to String.format("%.3f μS", skinConductance)))
}
