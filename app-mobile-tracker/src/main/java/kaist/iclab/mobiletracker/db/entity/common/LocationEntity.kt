package kaist.iclab.mobiletracker.db.entity.common

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index

/**
 * ObjectBox entity for location data from both phone and watch devices,
 * differentiated by [deviceType] (DeviceType.PHONE = 0, DeviceType.WATCH = 1).
 */
@Entity
data class LocationEntity(
    @Id var id: Long = 0,
    var eventId: String = "",
    var uuid: String = "",
    @Index var deviceType: Int = 0,
    var received: Long = 0,
    @Index var timestamp: Long = 0,
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var altitude: Double = 0.0,
    var speed: Float = 0f,
    var accuracy: Float = 0f
)
