package kaist.iclab.wearabletracker.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity
data class LocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val eventId: String = UUID.randomUUID().toString(),
    val received: Long,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val speed: Float,
    val accuracy: Float
) : CsvSerializable {
    override fun toCsvHeader(): String = "eventId,received,timestamp,latitude,longitude,altitude,speed,accuracy"
    override fun toCsvRow(): String = "$eventId,$received,$timestamp,$latitude,$longitude,$altitude,$speed,$accuracy"
}
