package kaist.iclab.mobiletracker.db.entity.common

import io.objectbox.annotation.Entity
import kaist.iclab.mobiletracker.db.entity.BaseEntity
import kaist.iclab.mobiletracker.db.entity.CsvSerializable
import kotlinx.serialization.Serializable

/**
 * ObjectBox entity for location data from both phone and watch devices, differentiated by
 * [deviceType]. Serializes directly into its Supabase row (replacing LocationSensorData +
 * LocationMapper/PhoneLocationMapper); unlike other sensors, [deviceType] is a real per-record
 * value (set at insert), not a constant.
 */
@Entity
@Serializable
class LocationEntity : BaseEntity, CsvSerializable {
    var latitude: Double = 0.0
    var longitude: Double = 0.0
    var altitude: Double = 0.0
    var speed: Float = 0f
    var accuracy: Float = 0f

    constructor() : super()

    constructor(
        id: Long = 0,
        eventId: String = "",
        uuid: String = "",
        received: Long = 0,
        timestamp: Long = 0,
        deviceType: Int = 0,
        latitude: Double = 0.0,
        longitude: Double = 0.0,
        altitude: Double = 0.0,
        speed: Float = 0f,
        accuracy: Float = 0f
    ) {
        initBaseEntity(id, eventId, uuid, received, timestamp, deviceType)
        this.latitude = latitude
        this.longitude = longitude
        this.altitude = altitude
        this.speed = speed
        this.accuracy = accuracy
    }

    override val csvHeader = "eventId,uuid,deviceType,received,timestamp,latitude,longitude,altitude,speed,accuracy"
    override fun toCsvRow() = "$eventId,$uuid,$deviceType,$received,$timestamp,$latitude,$longitude,$altitude,$speed,$accuracy"
}
