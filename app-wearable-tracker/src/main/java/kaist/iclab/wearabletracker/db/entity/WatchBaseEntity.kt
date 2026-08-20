package kaist.iclab.wearabletracker.db.entity

import io.objectbox.annotation.Id
import io.objectbox.annotation.Index
import java.util.UUID
import io.objectbox.annotation.BaseEntity as OBXBaseEntity

@OBXBaseEntity
abstract class WatchBaseEntity {
    @Id
    var id: Long = 0
    var eventId: String = ""
    var received: Long = 0
    @Index
    var timestamp: Long = 0

    protected fun initBase(received: Long, timestamp: Long) {
        this.eventId = UUID.randomUUID().toString()
        this.received = received
        this.timestamp = timestamp
    }
}
