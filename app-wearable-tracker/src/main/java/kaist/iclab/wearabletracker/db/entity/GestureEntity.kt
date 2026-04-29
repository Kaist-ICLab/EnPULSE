package kaist.iclab.wearabletracker.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity
data class GestureEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val eventId: String = UUID.randomUUID().toString(),
    val received: Long,
    override val timestamp: Long,
    val classIndex: Int,
    val score: Int,
    val probabilities: List<Int>
) : CsvSerializable {
    override fun toCsvHeader(): String = "eventId,received,timestamp,classIndex,score,probabilities"
    override fun toCsvRow(): String =
        "$eventId,$received,$timestamp,$classIndex,$score,${probabilities.joinToString(";")}"
}
