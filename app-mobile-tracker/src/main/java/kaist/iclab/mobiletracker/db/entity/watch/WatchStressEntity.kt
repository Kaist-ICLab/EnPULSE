package kaist.iclab.mobiletracker.db.entity.watch

import io.objectbox.annotation.Entity
import kaist.iclab.mobiletracker.data.DeviceType
import kaist.iclab.mobiletracker.db.entity.BaseEntity
import kaist.iclab.mobiletracker.db.entity.CsvSerializable
import kaist.iclab.mobiletracker.db.entity.RecordSerializable
import kaist.iclab.mobiletracker.repository.SensorRecord
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Locale

@Entity
@Serializable
class WatchStressEntity : BaseEntity, CsvSerializable, RecordSerializable {
    var rmssd: Float = 0f
    @SerialName("ibi_count")
    var ibiCount: Int = 0
    var threshold: Float = 0f
    @SerialName("is_stressed")
    var isStressed: Boolean = false

    constructor() : super()

    constructor(
        id: Long = 0,
        eventId: String = "",
        uuid: String = "",
        received: Long = 0,
        timestamp: Long = 0,
        deviceType: Int = DeviceType.WATCH.value,
        rmssd: Float = 0f,
        ibiCount: Int = 0,
        threshold: Float = 0f,
        isStressed: Boolean = false
    ) {
        initBaseEntity(id, eventId, uuid, received, timestamp, deviceType)
        this.rmssd = rmssd
        this.ibiCount = ibiCount
        this.threshold = threshold
        this.isStressed = isStressed
    }

    override fun csvHeader() = "eventId,uuid,received,timestamp,rmssd,ibiCount,threshold,isStressed"
    override fun toCsvRow() = "$eventId,$uuid,$received,$timestamp,$rmssd,$ibiCount,$threshold,$isStressed"

    override fun toRecord() = SensorRecord(
        id = id,
        timestamp = timestamp,
        fields = mapOf(
            "RMSSD" to String.format(Locale.getDefault(), "%.3f", rmssd),
            "IBI Count" to ibiCount.toString(),
            "Stressed" to isStressed.toString()
        )
    )
}
