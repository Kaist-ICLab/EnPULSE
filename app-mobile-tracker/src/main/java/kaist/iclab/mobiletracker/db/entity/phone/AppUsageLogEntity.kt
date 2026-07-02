package kaist.iclab.mobiletracker.db.entity.phone

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index

@Entity
data class AppUsageLogEntity(
    @Id var id: Long = 0,
    var uuid: String = "",
    var eventId: String = "",
    var received: Long = 0,
    @Index var timestamp: Long = 0,
    var packageName: String = "",
    var installedBy: String = "",
    var eventType: Int = 0
)
