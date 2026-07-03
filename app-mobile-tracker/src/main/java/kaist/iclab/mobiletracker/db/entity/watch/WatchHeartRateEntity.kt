package kaist.iclab.mobiletracker.db.entity.watch

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index
import kaist.iclab.mobiletracker.data.DeviceType
import kaist.iclab.mobiletracker.db.obx.EpochMillisIsoSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * ObjectBox entity for heart rate sensor data received from the watch.
 *
 * The inter-beat-interval lists are stored as `IntArray`, which ObjectBox persists natively
 * (no TypeConverter needed, unlike Room). `IntArray` serializes to the same JSON int-array as the
 * DTO's `List<Int>`, so no custom serializer is required for `ibi`/`ibiStatus`.
 */
@Entity
@Serializable
data class WatchHeartRateEntity(
    @Id
    @Transient
    var id: Long = 0,
    @SerialName("event_id")
    var eventId: String = "",
    var uuid: String = "",
    @Index
    @Serializable(with = EpochMillisIsoSerializer::class)
    var timestamp: Long = 0,
    @Serializable(with = EpochMillisIsoSerializer::class)
    var received: Long = 0,
    var hr: Int = 0,
    @SerialName("hr_status")
    var hrStatus: Int = 0,
    var ibi: IntArray = IntArray(0),
    @SerialName("ibi_status")
    var ibiStatus: IntArray = IntArray(0),
    @SerialName("device_type")
    var deviceType: Int = DeviceType.WATCH.value
) {
    // Arrays need structural equals/hashCode; generated data-class versions use identity.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WatchHeartRateEntity) return false
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
