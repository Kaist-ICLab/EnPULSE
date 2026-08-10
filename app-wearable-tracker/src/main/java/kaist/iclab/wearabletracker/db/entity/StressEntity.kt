package kaist.iclab.wearabletracker.db.entity

import io.objectbox.annotation.Entity

@Entity
class StressEntity : WatchBaseEntity, CsvSerializable {
    var rmssd1m: Float = 0f
    var ibiCount1m: Int = 0
    var rmssd5m: Float = 0f
    var ibiCount5m: Int = 0
    var threshold: Float = 0f
    var isStressed: Boolean = false

    constructor() : super()

    constructor(
        received: Long,
        timestamp: Long,
        rmssd1m: Float,
        ibiCount1m: Int,
        rmssd5m: Float,
        ibiCount5m: Int,
        threshold: Float,
        isStressed: Boolean
    ) : super() {
        initBase(received, timestamp)
        this.rmssd1m = rmssd1m
        this.ibiCount1m = ibiCount1m
        this.rmssd5m = rmssd5m
        this.ibiCount5m = ibiCount5m
        this.threshold = threshold
        this.isStressed = isStressed
    }

    override fun csvHeader(): String =
        "eventId,received,timestamp,rmssd1m,ibiCount1m,rmssd5m,ibiCount5m,threshold,isStressed"

    override fun toCsvRow(): String =
        "$eventId,$received,$timestamp,$rmssd1m,$ibiCount1m,$rmssd5m,$ibiCount5m,$threshold,$isStressed"
}
