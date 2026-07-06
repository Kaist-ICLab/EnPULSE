package kaist.iclab.mobiletracker.db.entity.phone

import io.objectbox.annotation.Entity
import kaist.iclab.mobiletracker.data.DeviceType
import kaist.iclab.mobiletracker.db.entity.BaseEntity
import kaist.iclab.mobiletracker.db.obx.JsonStringElementSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Entity
@Serializable
class AppListChangeEntity : BaseEntity {
    @SerialName("changed_app")
    @Serializable(with = JsonStringElementSerializer::class)
    var changedAppJson: String? = null

    @SerialName("app_list")
    @Serializable(with = JsonStringElementSerializer::class)
    var appListJson: String? = null

    constructor() : super()

    constructor(
        id: Long = 0,
        eventId: String = UUID.randomUUID().toString(),
        uuid: String = "",
        received: Long = 0,
        timestamp: Long = 0,
        deviceType: Int = DeviceType.PHONE.value,
        changedAppJson: String? = null,
        appListJson: String? = null
    ) {
        initBaseEntity(id, eventId, uuid, received, timestamp, deviceType)
        this.changedAppJson = changedAppJson
        this.appListJson = appListJson
    }
}
