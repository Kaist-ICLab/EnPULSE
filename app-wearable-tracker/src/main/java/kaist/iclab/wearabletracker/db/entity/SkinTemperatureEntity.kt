package kaist.iclab.wearabletracker.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity
data class SkinTemperatureEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val eventId: String = UUID.randomUUID().toString(),
    val received: Long,
    val timestamp: Long,
    val objectTemperature: Float,
    val ambientTemperature: Float,
    val status: Int
) : CsvSerializable {
    override fun toCsvHeader(): String = "eventId,received,timestamp,ambientTemp,objectTemp,status"
    override fun toCsvRow(): String = "$eventId,$received,$timestamp,$ambientTemperature,$objectTemperature,$status"
}
