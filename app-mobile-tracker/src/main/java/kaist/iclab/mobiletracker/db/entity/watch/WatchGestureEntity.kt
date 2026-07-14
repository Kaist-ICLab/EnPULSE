package kaist.iclab.mobiletracker.db.entity.watch

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class WatchGestureEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val eventId: String,
    val uuid: String?,
    val deviceType: Int = 1, // Watch
    val received: Long,
    val timestamp: Long,
    val classIndex: Int,
    val score: Int,
    val probabilities: List<Int>
)
