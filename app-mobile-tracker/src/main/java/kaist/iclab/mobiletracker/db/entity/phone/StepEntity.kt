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

@Entity
@Serializable
data class StepEntity(
    @Id
    @Transient
    var id: Long = 0,
    @SerialName("event_id")
    var eventId: String = UUID.randomUUID().toString(),
    var uuid: String = "",
    @Serializable(with = EpochMillisIsoSerializer::class)
    var received: Long = 0,
    @Index
    @Serializable(with = EpochMillisIsoSerializer::class)
    var timestamp: Long = 0,
    @SerialName("start_time")
    var startTime: Long = 0,
    @SerialName("end_time")
    var endTime: Long = 0,
    var steps: Long = 0,
    @SerialName("device_type")
    var deviceType: Int = DeviceType.PHONE.value
)
