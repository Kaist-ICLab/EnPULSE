package kaist.iclab.mobiletracker.db.entity.phone

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity
data class BluetoothScanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val eventId: String = UUID.randomUUID().toString(),
    val uuid: String,
    val received: Long,
    val timestamp: Long,
    val name: String,
    val alias: String,
    val address: String,
    val bondState: Int,
    val connectionType: Int,
    val classType: Int,
    val rssi: Int,
    val isLE: Boolean
)
