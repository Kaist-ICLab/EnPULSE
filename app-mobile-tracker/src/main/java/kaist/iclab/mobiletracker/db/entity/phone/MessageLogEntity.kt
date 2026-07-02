package kaist.iclab.mobiletracker.db.entity.phone

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index
import java.util.UUID

@Entity
data class MessageLogEntity(
    @Id var id: Long = 0,
    var eventId: String = UUID.randomUUID().toString(),
    var uuid: String = "",
    var received: Long = 0,
    @Index var timestamp: Long = 0,
    var number: String = "",
    var messageType: String = "",
    var contactType: Int = 0
)
