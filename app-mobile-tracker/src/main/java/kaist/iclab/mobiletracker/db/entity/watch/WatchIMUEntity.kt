package kaist.iclab.mobiletracker.db.entity.watch

import io.objectbox.annotation.Entity
import kaist.iclab.mobiletracker.data.DeviceType
import kaist.iclab.mobiletracker.db.entity.BaseEntity
import kaist.iclab.mobiletracker.db.entity.CsvSerializable
import kaist.iclab.mobiletracker.db.entity.RecordSerializable
import kaist.iclab.mobiletracker.repository.SensorRecord
import kotlinx.serialization.Serializable
import java.util.Locale

@Entity
@Serializable
class WatchIMUEntity : BaseEntity, CsvSerializable, RecordSerializable {
    var accX: Float = 0f
    var accY: Float = 0f
    var accZ: Float = 0f
    var gyroX: Float = 0f
    var gyroY: Float = 0f
    var gyroZ: Float = 0f

    constructor() : super()

    constructor(
        id: Long = 0,
        eventId: String = "",
        uuid: String = "",
        received: Long = 0,
        timestamp: Long = 0,
        deviceType: Int = DeviceType.WATCH.value,
        accX: Float = 0f,
        accY: Float = 0f,
        accZ: Float = 0f,
        gyroX: Float = 0f,
        gyroY: Float = 0f,
        gyroZ: Float = 0f
    ) {
        initBaseEntity(id, eventId, uuid, received, timestamp, deviceType)
        this.accX = accX
        this.accY = accY
        this.accZ = accZ
        this.gyroX = gyroX
        this.gyroY = gyroY
        this.gyroZ = gyroZ
    }

    override fun csvHeader() = "eventId,uuid,received,timestamp,accX,accY,accZ,gyroX,gyroY,gyroZ"
    override fun toCsvRow() =
        "$eventId,$uuid,$received,$timestamp,$accX,$accY,$accZ,$gyroX,$gyroY,$gyroZ"

    override fun toRecord() = SensorRecord(
        id = id,
        timestamp = timestamp,
        fields = mapOf(
            "AccX" to String.format(Locale.getDefault(), "%.3f", accX),
            "AccY" to String.format(Locale.getDefault(), "%.3f", accY),
            "AccZ" to String.format(Locale.getDefault(), "%.3f", accZ),
            "GyroX" to String.format(Locale.getDefault(), "%.3f", gyroX),
            "GyroY" to String.format(Locale.getDefault(), "%.3f", gyroY),
            "GyroZ" to String.format(Locale.getDefault(), "%.3f", gyroZ)
        )
    )
}
