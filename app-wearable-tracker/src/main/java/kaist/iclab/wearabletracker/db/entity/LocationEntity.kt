package kaist.iclab.wearabletracker.db.entity

import io.objectbox.annotation.Entity

@Entity
class LocationEntity() : WatchBaseEntity(), CsvSerializable {
    var latitude: Double = 0.0
    var longitude: Double = 0.0
    var altitude: Double = 0.0
    var speed: Float = 0f
    var accuracy: Float = 0f

    constructor(
        received: Long, timestamp: Long,
        latitude: Double, longitude: Double, altitude: Double,
        speed: Float, accuracy: Float
    ) : this() {
        initBase(received, timestamp)
        this.latitude = latitude; this.longitude = longitude; this.altitude = altitude
        this.speed = speed; this.accuracy = accuracy
    }

    override fun csvHeader() = "eventId,received,timestamp,latitude,longitude,altitude,speed,accuracy"
    override fun toCsvRow() = "$eventId,$received,$timestamp,$latitude,$longitude,$altitude,$speed,$accuracy"
}
