package kaist.iclab.wearabletracker.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity
data class EDAEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val eventId: String = UUID.randomUUID().toString(),
    val received: Long,
    val timestamp: Long,
    val skinConductance: Float,
    val status: Int
) : CsvSerializable {
    override fun toCsvHeader(): String = "eventId,received,timestamp,skinConductance,status"
    override fun toCsvRow(): String = "$eventId,$received,$timestamp,$skinConductance,$status"
}
