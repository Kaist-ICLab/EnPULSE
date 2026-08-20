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
class WifiScanEntity : BaseEntity, CsvSerializable, RecordSerializable {
    var ssid: String = ""
    var bssid: String = ""
    var frequency: Int = 0
    var level: Int = 0

    constructor() : super()

    constructor(
        id: Long = 0,
        eventId: String = UUID.randomUUID().toString(),
        uuid: String = "",
        received: Long = 0,
        timestamp: Long = 0,
        deviceType: Int = DeviceType.PHONE.value,
        ssid: String = "",
        bssid: String = "",
        frequency: Int = 0,
        level: Int = 0
    ) {
        initBaseEntity(id, eventId, uuid, received, timestamp, deviceType)
        this.ssid = ssid
        this.bssid = bssid
        this.frequency = frequency
        this.level = level
    }

    override fun csvHeader() = "eventId,uuid,received,timestamp,ssid,bssid,frequency,level"
    override fun toCsvRow(): String {
        val escapedSsid = ssid.replace("\"", "\"\"")
        return "$eventId,$uuid,$received,$timestamp,\"$escapedSsid\",$bssid,$frequency,$level"
    }

    override fun toRecord() = SensorRecord(
        id = id,
        timestamp = timestamp,
        fields = mapOf("SSID" to ssid, "BSSID" to bssid)
    )
}
