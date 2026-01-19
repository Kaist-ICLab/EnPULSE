package kaist.iclab.wearabletracker.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity
data class HeartRateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val eventId: String = UUID.randomUUID().toString(),
    val received: Long,
    val timestamp: Long,
    val hr: Int,
    val hrStatus: Int,
    val ibi: List<Int>,
    val ibiStatus: List<Int>,
) : CsvSerializable {
    override fun toCsvHeader(): String = "eventId,received,timestamp,hr,hrStatus,ibi,ibiStatus"
    override fun toCsvRow(): String {
        val ibiString = ibi.joinToString(";")
        val ibiStatusString = ibiStatus.joinToString(";")
        return "$eventId,$received,$timestamp,$hr,$hrStatus,$ibiString,$ibiStatusString"
    }
}
