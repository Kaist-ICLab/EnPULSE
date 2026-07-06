package kaist.iclab.mobiletracker.db.entity.phone

import io.objectbox.annotation.Entity
import kaist.iclab.mobiletracker.data.DeviceType
import kaist.iclab.mobiletracker.db.entity.BaseEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Entity
@Serializable
class BluetoothScanEntity : BaseEntity {
    var name: String = ""
    var alias: String = ""
    var address: String = ""

    @SerialName("bond_state")
    var bondState: Int = 0

    @SerialName("connection_type")
    var connectionType: Int = 0

    @SerialName("class_type")
    var classType: Int = 0

    var rssi: Int = 0

    @SerialName("is_le")
    var isLE: Boolean = false

    constructor() : super()

    constructor(
        id: Long = 0,
        eventId: String = UUID.randomUUID().toString(),
        uuid: String = "",
        received: Long = 0,
        timestamp: Long = 0,
        deviceType: Int = DeviceType.PHONE.value,
        name: String = "",
        alias: String = "",
        address: String = "",
        bondState: Int = 0,
        connectionType: Int = 0,
        classType: Int = 0,
        rssi: Int = 0,
        isLE: Boolean = false
    ) {
        initBaseEntity(id, eventId, uuid, received, timestamp, deviceType)
        this.name = name
        this.alias = alias
        this.address = address
        this.bondState = bondState
        this.connectionType = connectionType
        this.classType = classType
        this.rssi = rssi
        this.isLE = isLE
    }
}
