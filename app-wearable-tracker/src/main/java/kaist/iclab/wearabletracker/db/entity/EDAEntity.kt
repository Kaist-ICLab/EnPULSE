package kaist.iclab.wearabletracker.db.entity

import io.objectbox.annotation.Entity

@Entity
class EDAEntity() : WatchBaseEntity(), CsvSerializable {
    var skinConductance: Float = 0f
    var status: Int = 0

    constructor(received: Long, timestamp: Long, skinConductance: Float, status: Int) : this() {
        initBase(received, timestamp)
        this.skinConductance = skinConductance; this.status = status
    }

    override fun csvHeader() = "eventId,received,timestamp,skinConductance,status"
    override fun toCsvRow() = "$eventId,$received,$timestamp,$skinConductance,$status"
}
