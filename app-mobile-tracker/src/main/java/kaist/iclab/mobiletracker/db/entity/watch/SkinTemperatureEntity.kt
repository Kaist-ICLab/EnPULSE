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
class SkinTemperatureEntity : BaseEntity, CsvSerializable, RecordSerializable {
    @SerialName("ambient_temperature")
    var ambientTemp: Float = 0f

    @SerialName("object_temperature")
    var objectTemp: Float = 0f

    var status: Int = 0

    constructor() : super()

    constructor(
        id: Long = 0,
        eventId: String = "",
        uuid: String = "",
        received: Long = 0,
        timestamp: Long = 0,
        deviceType: Int = DeviceType.WATCH.value,
        ambientTemp: Float = 0f,
        objectTemp: Float = 0f,
        status: Int = 0
    ) {
        initBaseEntity(id, eventId, uuid, received, timestamp, deviceType)
        this.ambientTemp = ambientTemp
        this.objectTemp = objectTemp
        this.status = status
    }

    override fun csvHeader() = "eventId,uuid,received,timestamp,ambientTemp,objectTemp,status"
    override fun toCsvRow() = "$eventId,$uuid,$received,$timestamp,$ambientTemp,$objectTemp,$status"

    override fun toRecord() = SensorRecord(id = id, timestamp = timestamp, fields = mapOf("Skin Temp" to String.format("%.1f°C", objectTemp)))
}
