package kaist.iclab.mobiletracker.db.entity.common

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index
import kaist.iclab.mobiletracker.db.obx.EpochMillisIsoSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * ObjectBox entity for location data from both phone and watch devices, differentiated by
 * [deviceType]. Serializes directly into its Supabase row (replacing LocationSensorData +
 * LocationMapper/PhoneLocationMapper); unlike other sensors, [deviceType] is a real per-record
 * value (set at insert), not a constant.
 */
@Entity
@Serializable
data class LocationEntity(
    @Id
    @Transient
    var id: Long = 0,
    @SerialName("event_id")
    var eventId: String = "",
    var uuid: String = "",
    @Index
    @SerialName("device_type")
    var deviceType: Int = 0,
    @Serializable(with = EpochMillisIsoSerializer::class)
    var received: Long = 0,
    @Index
    @Serializable(with = EpochMillisIsoSerializer::class)
    var timestamp: Long = 0,
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var altitude: Double = 0.0,
    var speed: Float = 0f,
    var accuracy: Float = 0f
)
