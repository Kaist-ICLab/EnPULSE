package kaist.iclab.mobiletracker.db.entity

import io.objectbox.annotation.BaseEntity as OBXBaseEntity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index
import kaist.iclab.mobiletracker.db.obx.EpochMillisIsoSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@OBXBaseEntity
abstract class BaseEntity {
    @Id
    @Transient
    var id: Long = 0

    @SerialName("event_id")
    var eventId: String = ""

    var uuid: String = ""

    @Serializable(with = EpochMillisIsoSerializer::class)
    var received: Long = 0

    @Index
    @Serializable(with = EpochMillisIsoSerializer::class)
    var timestamp: Long = 0

    @Index
    @SerialName("device_type")
    var deviceType: Int = 0

    protected fun initBaseEntity(
        id: Long,
        eventId: String,
        uuid: String,
        received: Long,
        timestamp: Long,
        deviceType: Int
    ) {
        this.id = id
        this.eventId = eventId
        this.uuid = uuid
        this.received = received
        this.timestamp = timestamp
        this.deviceType = deviceType
    }
}
