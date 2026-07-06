package kaist.iclab.mobiletracker.db.entity.watch

import io.objectbox.annotation.Entity
import kaist.iclab.mobiletracker.data.DeviceType
import kaist.iclab.mobiletracker.db.entity.BaseEntity
import kotlinx.serialization.Serializable

@Entity
@Serializable
class WatchAccelerometerEntity : BaseEntity {
    var x: Float = 0f
    var y: Float = 0f
    var z: Float = 0f

    constructor() : super()

    constructor(
        id: Long = 0,
        eventId: String = "",
        uuid: String = "",
        received: Long = 0,
        timestamp: Long = 0,
        deviceType: Int = DeviceType.WATCH.value,
        x: Float = 0f,
        y: Float = 0f,
        z: Float = 0f
    ) {
        initBaseEntity(id, eventId, uuid, received, timestamp, deviceType)
        this.x = x
        this.y = y
        this.z = z
    }
}
