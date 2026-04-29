package kaist.iclab.wearabletracker.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity
data class StressEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val eventId: String = UUID.randomUUID().toString(),
    val received: Long,
    override val timestamp: Long,
    val windowStartMs: Long,
    val probability: Float,
    val isHighStress: Boolean
) : CsvSerializable {
    override fun toCsvHeader(): String =
        "eventId,received,timestamp,windowStartMs,probability,isHighStress"

    override fun toCsvRow(): String =
        "$eventId,$received,$timestamp,$windowStartMs,$probability,$isHighStress"
}
