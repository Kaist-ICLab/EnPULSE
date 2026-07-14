package kaist.iclab.wearabletracker.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class RmssdHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val timestamp: Long,
    val rmssd: Float,
)
