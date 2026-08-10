package kaist.iclab.wearabletracker.db.entity

import io.objectbox.annotation.Entity

@Entity
class StressEntity : WatchBaseEntity, CsvSerializable {
    var rmssd: Float = 0f
    var ibiCount: Int = 0
    var threshold: Float = 0f
    var isStressed: Boolean = false

    constructor() : super()

    constructor(
        received: Long,
        timestamp: Long,
        rmssd: Float,
        ibiCount: Int,
        threshold: Float,
        isStressed: Boolean
    ) : super() {
        initBase(received, timestamp)
        this.rmssd = rmssd
        this.ibiCount = ibiCount
        this.threshold = threshold
        this.isStressed = isStressed
    }

    override fun csvHeader(): String =
        "eventId,received,timestamp,rmssd,ibiCount,threshold,isStressed"

    override fun toCsvRow(): String =
        "$eventId,$received,$timestamp,$rmssd,$ibiCount,$threshold,$isStressed"
}
