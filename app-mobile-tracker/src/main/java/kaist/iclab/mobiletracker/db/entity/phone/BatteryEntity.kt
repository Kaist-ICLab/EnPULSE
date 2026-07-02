package kaist.iclab.mobiletracker.db.entity.phone

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index
import java.util.UUID

@Entity
data class BatteryEntity(
    @Id var id: Long = 0,
    var eventId: String = UUID.randomUUID().toString(),
    var uuid: String = "",
    @Index var timestamp: Long = 0,
    var received: Long = 0,
    var connectedType: Int = 0,
    var status: Int = 0,
    var level: Int = 0,
    var temperature: Int = 0
)
