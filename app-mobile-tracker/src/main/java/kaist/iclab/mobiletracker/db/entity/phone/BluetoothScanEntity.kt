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
data class BluetoothScanEntity(
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
    var name: String = "",
    var alias: String = "",
    var address: String = "",
    @SerialName("bond_state")
    var bondState: Int = 0,
    @SerialName("connection_type")
    var connectionType: Int = 0,
    @SerialName("class_type")
    var classType: Int = 0,
    var rssi: Int = 0,
    @SerialName("is_le")
    var isLE: Boolean = false,
    @SerialName("device_type")
    var deviceType: Int = DeviceType.PHONE.value
)
