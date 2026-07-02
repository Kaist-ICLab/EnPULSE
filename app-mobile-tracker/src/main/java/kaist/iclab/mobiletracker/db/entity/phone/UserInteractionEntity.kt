package kaist.iclab.mobiletracker.db.entity.phone

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index

@Entity
data class UserInteractionEntity(
    @Id var id: Long = 0,
    var eventId: String = "",
    var uuid: String = "",
    var received: Long = 0,
    @Index var timestamp: Long = 0,
    var packageName: String = "",
    var className: String = "",
    var eventType: Int = 0,
    var text: String = ""
)
