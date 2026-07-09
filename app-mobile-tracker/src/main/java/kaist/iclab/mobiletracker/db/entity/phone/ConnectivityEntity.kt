package kaist.iclab.mobiletracker.db.entity.phone

import io.objectbox.annotation.Entity
import kaist.iclab.mobiletracker.data.DeviceType
import kaist.iclab.mobiletracker.db.entity.BaseEntity
import kaist.iclab.mobiletracker.db.entity.CsvSerializable
import kaist.iclab.mobiletracker.db.obx.CommaJoinedListSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Entity
@Serializable
class ConnectivityEntity : BaseEntity, CsvSerializable {
    @SerialName("network_type")
    var networkType: String = ""

    @SerialName("is_connected")
    var isConnected: Boolean = false

    @SerialName("has_internet")
    var hasInternet: Boolean = false

    @SerialName("transport_types")
    @Serializable(with = CommaJoinedListSerializer::class)
    var transportTypes: String = ""

    constructor() : super()

    constructor(
        id: Long = 0,
        eventId: String = UUID.randomUUID().toString(),
        uuid: String = "",
        received: Long = 0,
        timestamp: Long = 0,
        deviceType: Int = DeviceType.PHONE.value,
        networkType: String = "",
        isConnected: Boolean = false,
        hasInternet: Boolean = false,
        transportTypes: String = ""
    ) {
        initBaseEntity(id, eventId, uuid, received, timestamp, deviceType)
        this.networkType = networkType
        this.isConnected = isConnected
        this.hasInternet = hasInternet
        this.transportTypes = transportTypes
    }

    override val csvHeader = "eventId,uuid,received,timestamp,networkType,isConnected,hasInternet,transportTypes"
    override fun toCsvRow() = "$eventId,$uuid,$received,$timestamp,$networkType,$isConnected,$hasInternet,$transportTypes"
}
