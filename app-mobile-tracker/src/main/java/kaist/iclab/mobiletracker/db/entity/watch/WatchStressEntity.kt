package kaist.iclab.mobiletracker.db.entity.watch

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class WatchStressEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val eventId: String,
    val uuid: String?,
    val deviceType: Int = 1, // Watch
    val received: Long,
    val timestamp: Long,
    val windowStartMs: Long,
    val probability: Float,
    val isHighStress: Boolean
)
