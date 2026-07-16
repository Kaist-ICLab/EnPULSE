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
class WatchStressEntity : BaseEntity, CsvSerializable, RecordSerializable {
    @SerialName("window_start")
    var windowStart: Long = 0
    @SerialName("window_end")
    var windowEnd: Long = 0
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
        windowStart: Long = 0,
        windowEnd: Long = 0,
        rmssd: Float = 0f,
        ibiCount: Int = 0,
        threshold: Float = 0f,
        isStressed: Boolean = false
    ) {
        initBaseEntity(id, eventId, uuid, received, timestamp, deviceType)
        this.windowStart = windowStart
        this.windowEnd = windowEnd
        this.rmssd = rmssd
        this.ibiCount = ibiCount
        this.threshold = threshold
        this.isStressed = isStressed
    }

    override fun csvHeader() = "eventId,uuid,received,timestamp,windowStart,windowEnd,rmssd,ibiCount,threshold,isStressed"
    override fun toCsvRow() = "$eventId,$uuid,$received,$timestamp,$windowStart,$windowEnd,$rmssd,$ibiCount,$threshold,$isStressed"

    override fun toRecord() = SensorRecord(
        id = id,
        timestamp = timestamp,
        fields = mapOf(
            "RMSSD" to String.format("%.3f", rmssd),
            "IBI Count" to ibiCount.toString(),
            "Stressed" to isStressed.toString()
        )
    )
}
