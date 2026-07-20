package kaist.iclab.wearabletracker.db.entity

import io.objectbox.annotation.Entity

@Entity
class ECGEntity() : WatchBaseEntity(), CsvSerializable {
    var ecgMv: Float = 0f
    var leadOff: Int = 0
    var sequence: Int = 0
    var ppgGreen: Int = 0
    var maxThresholdMv: Float = 0f
    var minThresholdMv: Float = 0f

    constructor(
        received: Long, timestamp: Long,
        ecgMv: Float, leadOff: Int, sequence: Int, ppgGreen: Int,
        maxThresholdMv: Float, minThresholdMv: Float
    ) : this() {
        initBase(received, timestamp)
        this.ecgMv = ecgMv; this.leadOff = leadOff; this.sequence = sequence; this.ppgGreen = ppgGreen
        this.maxThresholdMv = maxThresholdMv; this.minThresholdMv = minThresholdMv
    }

    override fun csvHeader() = "eventId,received,timestamp,ecgMv,leadOff,sequence,ppgGreen,maxThresholdMv,minThresholdMv"
    override fun toCsvRow() = "$eventId,$received,$timestamp,$ecgMv,$leadOff,$sequence,$ppgGreen,$maxThresholdMv,$minThresholdMv"
}
