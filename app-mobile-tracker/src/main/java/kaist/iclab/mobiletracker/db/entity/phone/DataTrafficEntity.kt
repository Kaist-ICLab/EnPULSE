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
class DataTrafficEntity : BaseEntity, CsvSerializable, RecordSerializable {
    @SerialName("total_rx")
    var totalRx: Long = 0

    @SerialName("total_tx")
    var totalTx: Long = 0

    @SerialName("mobile_rx")
    var mobileRx: Long = 0

    @SerialName("mobile_tx")
    var mobileTx: Long = 0

    constructor() : super()

    constructor(
        id: Long = 0,
        eventId: String = UUID.randomUUID().toString(),
        uuid: String = "",
        received: Long = 0,
        timestamp: Long = 0,
        deviceType: Int = DeviceType.PHONE.value,
        totalRx: Long = 0,
        totalTx: Long = 0,
        mobileRx: Long = 0,
        mobileTx: Long = 0
    ) {
        initBaseEntity(id, eventId, uuid, received, timestamp, deviceType)
        this.totalRx = totalRx
        this.totalTx = totalTx
        this.mobileRx = mobileRx
        this.mobileTx = mobileTx
    }

    override val csvHeader = "eventId,uuid,received,timestamp,totalRx,totalTx,mobileRx,mobileTx"
    override fun toCsvRow() = "$eventId,$uuid,$received,$timestamp,$totalRx,$totalTx,$mobileRx,$mobileTx"

    override fun toRecord() = SensorRecord(id = id, timestamp = timestamp, fields = mapOf("Total Rx" to "${totalRx / 1024} KB", "Total Tx" to "${totalTx / 1024} KB"))
}
