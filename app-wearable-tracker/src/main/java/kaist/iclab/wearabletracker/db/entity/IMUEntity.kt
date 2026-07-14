package kaist.iclab.wearabletracker.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity
data class IMUEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val eventId: String = UUID.randomUUID().toString(),
    val received: Long,
    override val timestamp: Long,
    val accX: Float,
    val accY: Float,
    val accZ: Float,
    val gyroX: Float,
    val gyroY: Float,
    val gyroZ: Float
) : CsvSerializable {
    override fun toCsvHeader(): String =
        "eventId,received,timestamp,accX,accY,accZ,gyroX,gyroY,gyroZ"

    override fun toCsvRow(): String =
        "$eventId,$received,$timestamp,$accX,$accY,$accZ,$gyroX,$gyroY,$gyroZ"
}
