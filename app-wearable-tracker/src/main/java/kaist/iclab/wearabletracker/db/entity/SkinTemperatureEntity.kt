package kaist.iclab.wearabletracker.db.entity

import io.objectbox.annotation.Entity

@Entity
class SkinTemperatureEntity() : WatchBaseEntity(), CsvSerializable {
    var objectTemperature: Float = 0f
    var ambientTemperature: Float = 0f
    var status: Int = 0

    constructor(
        received: Long, timestamp: Long,
        objectTemperature: Float, ambientTemperature: Float, status: Int
    ) : this() {
        initBase(received, timestamp)
        this.objectTemperature = objectTemperature
        this.ambientTemperature = ambientTemperature
        this.status = status
    }

    override fun csvHeader() = "eventId,received,timestamp,ambientTemp,objectTemp,status"
    override fun toCsvRow() =
        "$eventId,$received,$timestamp,$ambientTemperature,$objectTemperature,$status"
}
