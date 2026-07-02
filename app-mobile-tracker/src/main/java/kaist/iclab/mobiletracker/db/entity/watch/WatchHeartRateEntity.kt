package kaist.iclab.mobiletracker.db.entity.watch

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index

/**
 * ObjectBox entity for heart rate sensor data received from the watch.
 *
 * The inter-beat-interval lists are stored as `IntArray`, which ObjectBox persists natively
 * (no TypeConverter needed, unlike Room).
 */
@Entity
data class WatchHeartRateEntity(
    @Id var id: Long = 0,
    var eventId: String = "",
    var uuid: String = "",
    @Index var timestamp: Long = 0,
    var received: Long = 0,
    var hr: Int = 0,
    var hrStatus: Int = 0,
    var ibi: IntArray = IntArray(0),
    var ibiStatus: IntArray = IntArray(0)
) {
    // Arrays need structural equals/hashCode; generated data-class versions use identity.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WatchHeartRateEntity) return false
        return id == other.id && eventId == other.eventId && uuid == other.uuid &&
            timestamp == other.timestamp && received == other.received &&
            hr == other.hr && hrStatus == other.hrStatus &&
            ibi.contentEquals(other.ibi) && ibiStatus.contentEquals(other.ibiStatus)
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
        return result
    }
}
