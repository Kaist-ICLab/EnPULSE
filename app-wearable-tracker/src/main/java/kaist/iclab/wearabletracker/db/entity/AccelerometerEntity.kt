package kaist.iclab.wearabletracker.db.entity

import io.objectbox.annotation.Entity

@Entity
class AccelerometerEntity() : WatchBaseEntity(), CsvSerializable {
    var x: Float = 0f
    var y: Float = 0f
    var z: Float = 0f

    constructor(received: Long, timestamp: Long, x: Float, y: Float, z: Float) : this() {
        initBase(received, timestamp)
        this.x = x; this.y = y; this.z = z
    }

    override fun toCsvHeader() = "eventId,received,timestamp,x,y,z"
    override fun toCsvRow() = "$eventId,$received,$timestamp,$x,$y,$z"
}
