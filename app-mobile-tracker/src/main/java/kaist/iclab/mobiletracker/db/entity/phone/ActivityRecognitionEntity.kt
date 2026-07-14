package kaist.iclab.mobiletracker.db.entity.phone

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity
data class ActivityRecognitionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val eventId: String = UUID.randomUUID().toString(),
    val uuid: String,
    val received: Long,
    val timestamp: Long,
    val elapsedRealtimeMillis: Long,
    val activityType: Int,
    val score: Int,
    val probabilities: List<Int>
)
