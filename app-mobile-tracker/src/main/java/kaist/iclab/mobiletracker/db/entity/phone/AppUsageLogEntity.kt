package kaist.iclab.mobiletracker.db.entity.phone

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index
import kaist.iclab.mobiletracker.data.DeviceType
import kaist.iclab.mobiletracker.db.obx.EpochMillisIsoSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Entity
@Serializable
data class AppUsageLogEntity(
    @Id
    @Transient
    var id: Long = 0,
    var uuid: String = "",
    @SerialName("event_id")
    var eventId: String = "",
    @Serializable(with = EpochMillisIsoSerializer::class)
    var received: Long = 0,
    @Index
    @Serializable(with = EpochMillisIsoSerializer::class)
    var timestamp: Long = 0,
    @SerialName("device_type")
    var deviceType: Int = DeviceType.PHONE.value,
    @SerialName("package_name")
    var packageName: String = "",
    @SerialName("installed_by")
    var installedBy: String = "",
    @SerialName("event_type")
    var eventType: Int = 0
)
