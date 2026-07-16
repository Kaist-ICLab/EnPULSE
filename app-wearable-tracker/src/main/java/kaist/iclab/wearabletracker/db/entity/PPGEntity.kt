package kaist.iclab.wearabletracker.db.entity

import io.objectbox.annotation.Entity

@Entity
class PPGEntity() : WatchBaseEntity(), CsvSerializable {
    var green: Int = 0
    var red: Int = 0
    var ir: Int = 0
    var greenStatus: Int = 0
    var redStatus: Int = 0
    var irStatus: Int = 0

    constructor(
        received: Long, timestamp: Long,
        green: Int, red: Int, ir: Int,
        greenStatus: Int, redStatus: Int, irStatus: Int
    ) : this() {
        initBase(received, timestamp)
        this.green = green; this.red = red; this.ir = ir
        this.greenStatus = greenStatus; this.redStatus = redStatus; this.irStatus = irStatus
    }

    override fun csvHeader() = "eventId,received,timestamp,green,greenStatus,red,redStatus,ir,irStatus"
    override fun toCsvRow() = "$eventId,$received,$timestamp,$green,$greenStatus,$red,$redStatus,$ir,$irStatus"
}
