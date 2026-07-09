package kaist.iclab.wearabletracker.db.entity

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

@Entity
data class RmssdHistoryEntity(
    @Id var id: Long = 0,
    var timestamp: Long = 0,
    var rmssd: Float = 0f
)
