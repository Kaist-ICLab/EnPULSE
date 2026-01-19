package kaist.iclab.mobiletracker.db.entity.phone

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity
data class MediaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val eventId: String = UUID.randomUUID().toString(),
    val uuid: String,
    val received: Long,
    val timestamp: Long,
    val operation: String,
    val mediaType: String,
    val storageType: String,
    val uri: String,
    val fileName: String?,
    val mimeType: String?,
    val size: Long?,
    val dateAdded: Long?,
    val dateModified: Long?
)
