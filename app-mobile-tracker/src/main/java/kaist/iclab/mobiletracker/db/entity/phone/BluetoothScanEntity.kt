package kaist.iclab.mobiletracker.db.entity.phone

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index
import java.util.UUID

@Entity
data class BluetoothScanEntity(
    @Id var id: Long = 0,
    var eventId: String = UUID.randomUUID().toString(),
    var uuid: String = "",
    var received: Long = 0,
    @Index var timestamp: Long = 0,
    var name: String = "",
    var alias: String = "",
    var address: String = "",
    var bondState: Int = 0,
    var connectionType: Int = 0,
    var classType: Int = 0,
    var rssi: Int = 0,
    var isLE: Boolean = false
)
