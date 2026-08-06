package kaist.iclab.mobiletracker.db.entity.phone

import io.objectbox.annotation.Entity
import kaist.iclab.mobiletracker.data.DeviceType
import kaist.iclab.mobiletracker.db.entity.BaseEntity
import kaist.iclab.mobiletracker.db.entity.CsvSerializable
import kaist.iclab.mobiletracker.db.entity.RecordSerializable
import kaist.iclab.mobiletracker.db.obx.JsonStringElementSerializer
import kaist.iclab.mobiletracker.repository.SensorRecord
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * A generic event logged by a third-party webapp via the `logEvent` bridge action
 * ([kaist.iclab.mobiletracker.webapp.bridge.LogBridgeHandler]). Stored and uploaded to Supabase
 * through the same [kaist.iclab.mobiletracker.di.SensorDescriptor] machinery as every other sensor.
 */
@Entity
@Serializable
class WebAppLogEntity : BaseEntity, CsvSerializable, RecordSerializable {
    @SerialName("web_app_id")
    var webAppId: String = ""

    @SerialName("event_name")
    var eventName: String = ""

    @SerialName("properties")
    @Serializable(with = JsonStringElementSerializer::class)
    var propertiesJson: String? = null

    constructor() : super()

    constructor(
        id: Long = 0,
        eventId: String = UUID.randomUUID().toString(),
        uuid: String = "",
        received: Long = 0,
        timestamp: Long = 0,
        deviceType: Int = DeviceType.PHONE.value,
        webAppId: String = "",
        eventName: String = "",
        propertiesJson: String? = null
    ) {
        initBaseEntity(id, eventId, uuid, received, timestamp, deviceType)
        this.webAppId = webAppId
        this.eventName = eventName
        this.propertiesJson = propertiesJson
    }

    override fun csvHeader() = "eventId,uuid,received,timestamp,webAppId,eventName,propertiesJson"
    override fun toCsvRow(): String {
        val escapedProperties = propertiesJson?.replace("\"", "\"\"") ?: ""
        return "$eventId,$uuid,$received,$timestamp,$webAppId,$eventName,\"$escapedProperties\""
    }

    override fun toRecord() = SensorRecord(
        id = id,
        timestamp = timestamp,
        fields = mapOf("Event" to eventName, "WebApp" to webAppId)
    )
}
