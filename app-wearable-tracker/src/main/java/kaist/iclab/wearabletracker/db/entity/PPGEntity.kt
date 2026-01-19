package kaist.iclab.wearabletracker.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity
data class PPGEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val eventId: String = UUID.randomUUID().toString(),
    val received: Long,
    val timestamp: Long,
    val green: Int,
    val red: Int,
    val ir: Int,
    val greenStatus: Int,
    val redStatus: Int,
    val irStatus: Int,
) : CsvSerializable {
    override fun toCsvHeader(): String = "eventId,received,timestamp,green,greenStatus,red,redStatus,ir,irStatus"
    override fun toCsvRow(): String = "$eventId,$received,$timestamp,$green,$greenStatus,$red,$redStatus,$ir,$irStatus"
}
