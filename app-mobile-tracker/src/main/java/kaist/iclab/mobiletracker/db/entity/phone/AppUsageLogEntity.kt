package kaist.iclab.mobiletracker.db.entity.phone

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
class AppUsageLogEntity : BaseEntity, CsvSerializable, RecordSerializable {
    @SerialName("package_name")
    var packageName: String = ""

    @SerialName("installed_by")
    var installedBy: String = ""

    @SerialName("event_type")
    var eventType: Int = 0

    constructor() : super()

    constructor(
        id: Long = 0,
        eventId: String = "",
        uuid: String = "",
        received: Long = 0,
        timestamp: Long = 0,
        deviceType: Int = DeviceType.PHONE.value,
        packageName: String = "",
        installedBy: String = "",
        eventType: Int = 0
    ) {
        initBaseEntity(id, eventId, uuid, received, timestamp, deviceType)
        this.packageName = packageName
        this.installedBy = installedBy
        this.eventType = eventType
    }

    override fun csvHeader() = "eventId,uuid,received,timestamp,packageName,installedBy,eventType"
    override fun toCsvRow() =
        "$eventId,$uuid,$received,$timestamp,$packageName,$installedBy,$eventType"

    override fun toRecord() = SensorRecord(
        id = id,
        timestamp = timestamp,
        fields = mapOf("Package" to packageName, "Event" to eventType.toString())
    )
}
