package kaist.iclab.mobiletracker.db.entity.phone

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index
import kaist.iclab.mobiletracker.data.DeviceType
import kaist.iclab.mobiletracker.db.obx.EpochMillisIsoSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.UUID

/**
 * ObjectBox entity that also serializes directly into its Supabase row (replacing the separate
 * BatterySensorData DTO + BatteryMapper). `@Transient id` is stored locally but omitted from the
 * upload JSON; `uuid` is overwritten with the logged-in user's UUID at upload time.
 */
@Entity
@Serializable
data class BatteryEntity(
    @Id
    @Transient
    var id: Long = 0,
    @SerialName("event_id")
    var eventId: String = UUID.randomUUID().toString(),
    var uuid: String = "",
    @Index
    @Serializable(with = EpochMillisIsoSerializer::class)
    var timestamp: Long = 0,
    @Serializable(with = EpochMillisIsoSerializer::class)
    var received: Long = 0,
    @SerialName("device_type")
    var deviceType: Int = DeviceType.PHONE.value,
    @SerialName("connected_type")
    var connectedType: Int = 0,
    var status: Int = 0,
    var level: Int = 0,
    var temperature: Int = 0
)
