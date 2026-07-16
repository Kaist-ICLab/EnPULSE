package kaist.iclab.wearabletracker.db.entity

import io.objectbox.annotation.Entity

@Entity
class IMUEntity : WatchBaseEntity, CsvSerializable {
    var accX: Float = 0f
    var accY: Float = 0f
    var accZ: Float = 0f
    var gyroX: Float = 0f
    var gyroY: Float = 0f
    var gyroZ: Float = 0f

    constructor() : super()

    constructor(
        received: Long,
        timestamp: Long,
        accX: Float,
        accY: Float,
        accZ: Float,
        gyroX: Float,
        gyroY: Float,
        gyroZ: Float
    ) : super() {
        initBase(received, timestamp)
        this.accX = accX
        this.accY = accY
        this.accZ = accZ
        this.gyroX = gyroX
        this.gyroY = gyroY
        this.gyroZ = gyroZ
    }

    override fun toCsvHeader(): String =
        "eventId,received,timestamp,accX,accY,accZ,gyroX,gyroY,gyroZ"

    override fun toCsvRow(): String =
        "$eventId,$received,$timestamp,$accX,$accY,$accZ,$gyroX,$gyroY,$gyroZ"
}
