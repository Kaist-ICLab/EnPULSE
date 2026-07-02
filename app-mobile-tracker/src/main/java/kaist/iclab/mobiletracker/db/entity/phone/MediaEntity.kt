package kaist.iclab.mobiletracker.db.entity.phone

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index
import java.util.UUID

@Entity
data class MediaEntity(
    @Id var id: Long = 0,
    var eventId: String = UUID.randomUUID().toString(),
    var uuid: String = "",
    var received: Long = 0,
    @Index var timestamp: Long = 0,
    var operation: String = "",
    var mediaType: String = "",
    var storageType: String = "",
    var uri: String = "",
    var fileName: String? = null,
    var mimeType: String? = null,
    var size: Long? = null,
    var dateAdded: Long? = null,
    var dateModified: Long? = null
)
