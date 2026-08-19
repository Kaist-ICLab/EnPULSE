package kaist.iclab.mobiletracker.db.entity.common

import io.objectbox.annotation.Entity
import kaist.iclab.mobiletracker.db.entity.BaseEntity
import kaist.iclab.mobiletracker.db.entity.CsvSerializable
import kaist.iclab.mobiletracker.db.entity.RecordSerializable
import kaist.iclab.mobiletracker.repository.SensorRecord
import kotlinx.serialization.Serializable
import java.util.Locale
import java.util.UUID
import kotlin.String

/**
 * ObjectBox entity for location data from both phone and watch devices, differentiated by
 * [deviceType]. Serializes directly into its Supabase row (replacing LocationSensorData +
 * LocationMapper/PhoneLocationMapper); unlike other sensors, [deviceType] is a real per-record
 * value (set at insert), not a constant.
 */
@Entity
@Serializable
class LocationEntity : BaseEntity, CsvSerializable, RecordSerializable {
    var latitude: Double = 0.0
    var longitude: Double = 0.0
    var altitude: Double = 0.0
    var speed: Float = 0f
    var accuracy: Float = 0f

    constructor() : super()

    constructor(
        id: Long = 0,
        eventId: String = UUID.randomUUID().toString(),
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

    override fun csvHeader() = "eventId,uuid,deviceType,received,timestamp,latitude,longitude,altitude,speed,accuracy"
    override fun toCsvRow() = "$eventId,$uuid,$deviceType,$received,$timestamp,$latitude,$longitude,$altitude,$speed,$accuracy"

    override fun toRecord() = SensorRecord(
        id = id,
        timestamp = timestamp,
        fields = mapOf(
            "Latitude" to String.format(Locale.getDefault(), "%.6f°", latitude),
            "Longitude" to String.format(Locale.getDefault(), "%.6f°", longitude),
            "Accuracy" to String.format(Locale.getDefault(), "%.1f m", accuracy)
        )
    )
}
