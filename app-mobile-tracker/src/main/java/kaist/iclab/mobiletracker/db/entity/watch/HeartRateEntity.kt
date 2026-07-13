package kaist.iclab.mobiletracker.db.entity.watch

import io.objectbox.annotation.Entity
import kaist.iclab.mobiletracker.data.DeviceType
import kaist.iclab.mobiletracker.db.entity.BaseEntity
import kaist.iclab.mobiletracker.db.entity.CsvSerializable
import kaist.iclab.mobiletracker.db.entity.RecordSerializable
import kaist.iclab.mobiletracker.repository.SensorRecord
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * ObjectBox entity for heart rate sensor data received from the watch.
 *
 * The inter-beat-interval lists are stored as `IntArray`, which ObjectBox persists natively
 * (no TypeConverter needed, unlike Room). `IntArray` serializes to the same JSON int-array as the
 * DTO's `List<Int>`, so no custom serializer is required for `ibi`/`ibiStatus`.
 */
@Entity
@Serializable
class HeartRateEntity : BaseEntity, CsvSerializable, RecordSerializable {
    var hr: Int = 0

    @SerialName("hr_status")
    var hrStatus: Int = 0

    var ibi: IntArray = IntArray(0)

    @SerialName("ibi_status")
    var ibiStatus: IntArray = IntArray(0)

    constructor() : super()

    constructor(
        id: Long = 0,
        eventId: String = "",
        uuid: String = "",
        received: Long = 0,
        timestamp: Long = 0,
        deviceType: Int = DeviceType.WATCH.value,
        hr: Int = 0,
        hrStatus: Int = 0,
        ibi: IntArray = IntArray(0),
        ibiStatus: IntArray = IntArray(0)
    ) {
        initBaseEntity(id, eventId, uuid, received, timestamp, deviceType)
        this.hr = hr
        this.hrStatus = hrStatus
        this.ibi = ibi
        this.ibiStatus = ibiStatus
    }

    override fun csvHeader() = "eventId,uuid,received,timestamp,hr,hrStatus,ibi,ibiStatus"
    override fun toCsvRow(): String {
        val escapedIbi = ibi.joinToString(",").replace("\"", "\"\"")
        val escapedIbiStatus = ibiStatus.joinToString(",").replace("\"", "\"\"")
        return "$eventId,$uuid,$received,$timestamp,$hr,$hrStatus,\"[$escapedIbi]\",\"[$escapedIbiStatus]\""
    }

    override fun toRecord() = SensorRecord(id = id, timestamp = timestamp, fields = mapOf("Heart Rate" to "$hr BPM", "Status" to hrStatus.toString()))

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HeartRateEntity) return false
        return id == other.id && eventId == other.eventId && uuid == other.uuid &&
            timestamp == other.timestamp && received == other.received &&
            hr == other.hr && hrStatus == other.hrStatus &&
            ibi.contentEquals(other.ibi) && ibiStatus.contentEquals(other.ibiStatus) &&
            deviceType == other.deviceType
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + eventId.hashCode()
        result = 31 * result + uuid.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + received.hashCode()
        result = 31 * result + hr
        result = 31 * result + hrStatus
        result = 31 * result + ibi.contentHashCode()
        result = 31 * result + ibiStatus.contentHashCode()
        result = 31 * result + deviceType
        return result
    }
}
