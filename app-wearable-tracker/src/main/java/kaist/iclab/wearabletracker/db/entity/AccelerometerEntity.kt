package kaist.iclab.wearabletracker.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity
data class AccelerometerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val eventId: String = UUID.randomUUID().toString(),
    val received: Long,
    val timestamp: Long,
    val x: Float,
    val y: Float,
    val z: Float
) : CsvSerializable {
    override fun toCsvHeader(): String = "eventId,received,timestamp,x,y,z"
    override fun toCsvRow(): String = "$eventId,$received,$timestamp,$x,$y,$z"
}
