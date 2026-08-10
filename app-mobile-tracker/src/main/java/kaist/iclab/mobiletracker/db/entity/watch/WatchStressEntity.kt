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
    @SerialName("rmssd_1m")
    var rmssd1m: Float = 0f
    @SerialName("ibi_count_1m")
    var ibiCount1m: Int = 0
    @SerialName("rmssd_5m")
    var rmssd5m: Float = 0f
    @SerialName("ibi_count_5m")
    var ibiCount5m: Int = 0
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
        rmssd1m: Float = 0f,
        ibiCount1m: Int = 0,
        rmssd5m: Float = 0f,
        ibiCount5m: Int = 0,
        threshold: Float = 0f,
        isStressed: Boolean = false
    ) {
        initBaseEntity(id, eventId, uuid, received, timestamp, deviceType)
        this.rmssd1m = rmssd1m
        this.ibiCount1m = ibiCount1m
        this.rmssd5m = rmssd5m
        this.ibiCount5m = ibiCount5m
        this.threshold = threshold
        this.isStressed = isStressed
    }

    override fun csvHeader() =
        "eventId,uuid,received,timestamp,rmssd1m,ibiCount1m,rmssd5m,ibiCount5m,threshold,isStressed"
    override fun toCsvRow() =
        "$eventId,$uuid,$received,$timestamp,$rmssd1m,$ibiCount1m,$rmssd5m,$ibiCount5m,$threshold,$isStressed"

    override fun toRecord() = SensorRecord(
        id = id,
        timestamp = timestamp,
        fields = mapOf(
            "RMSSD (1m)" to String.format(Locale.getDefault(), "%.3f", rmssd1m),
            "RMSSD (5m)" to String.format(Locale.getDefault(), "%.3f", rmssd5m),
            "IBI Count (5m)" to ibiCount5m.toString(),
            "Stressed" to isStressed.toString()
        )
    )
}
