package kaist.iclab.mobiletracker.db.entity.phone

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity
data class WifiScanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val eventId: String = UUID.randomUUID().toString(),
    val uuid: String,
    val received: Long,
    val timestamp: Long,
    val ssid: String,
    val bssid: String,
    val frequency: Int,
    val level: Int
)