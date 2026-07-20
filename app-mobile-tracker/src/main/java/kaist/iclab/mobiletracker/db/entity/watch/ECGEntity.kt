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
class ECGEntity : BaseEntity, CsvSerializable, RecordSerializable {
    @SerialName("ecg_mv")
    var ecgMv: Float = 0f

    @SerialName("lead_off")
    var leadOff: Int = 0

    var sequence: Int = 0

    @SerialName("ppg_green")
    var ppgGreen: Int = 0

    @SerialName("max_threshold_mv")
    var maxThresholdMv: Float = 0f

    @SerialName("min_threshold_mv")
    var minThresholdMv: Float = 0f

    constructor() : super()

    constructor(
        id: Long = 0,
        eventId: String = "",
        uuid: String = "",
        received: Long = 0,
        timestamp: Long = 0,
        deviceType: Int = DeviceType.WATCH.value,
        ecgMv: Float = 0f,
        leadOff: Int = 0,
        sequence: Int = 0,
        ppgGreen: Int = 0,
        maxThresholdMv: Float = 0f,
        minThresholdMv: Float = 0f
    ) {
        initBaseEntity(id, eventId, uuid, received, timestamp, deviceType)
        this.ecgMv = ecgMv
        this.leadOff = leadOff
        this.sequence = sequence
        this.ppgGreen = ppgGreen
        this.maxThresholdMv = maxThresholdMv
        this.minThresholdMv = minThresholdMv
    }

    override fun csvHeader() = "eventId,uuid,received,timestamp,ecgMv,leadOff,sequence,ppgGreen,maxThresholdMv,minThresholdMv"
    override fun toCsvRow() = "$eventId,$uuid,$received,$timestamp,$ecgMv,$leadOff,$sequence,$ppgGreen,$maxThresholdMv,$minThresholdMv"

    override fun toRecord() = SensorRecord(
        id = id,
        timestamp = timestamp,
        fields = mapOf("ECG (mV)" to ecgMv.toString(), "Lead Off" to leadOff.toString())
    )
}
