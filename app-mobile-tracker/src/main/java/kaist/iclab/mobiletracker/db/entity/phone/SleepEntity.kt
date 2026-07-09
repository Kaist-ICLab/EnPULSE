package kaist.iclab.mobiletracker.db.entity.phone

import io.objectbox.annotation.Entity
import kaist.iclab.mobiletracker.data.DeviceType
import kaist.iclab.mobiletracker.db.entity.BaseEntity
import kaist.iclab.mobiletracker.db.entity.CsvSerializable
import kaist.iclab.mobiletracker.db.entity.RecordSerializable
import kaist.iclab.mobiletracker.db.obx.JsonStringElementSerializer
import kaist.iclab.mobiletracker.repository.SensorRecord
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Entity
@Serializable
class SleepEntity : BaseEntity, CsvSerializable, RecordSerializable {
    @SerialName("start_time")
    var startTime: Long = 0

    @SerialName("end_time")
    var endTime: Long = 0

    @SerialName("duration_seconds")
    var durationSeconds: Long = 0

    @SerialName("sleep_score")
    var sleepScore: Int? = null

    @SerialName("stages")
    @Serializable(with = JsonStringElementSerializer::class)
    var stagesJson: String? = null

    constructor() : super()

    constructor(
        id: Long = 0,
        eventId: String = UUID.randomUUID().toString(),
        uuid: String = "",
        received: Long = 0,
        timestamp: Long = 0,
        deviceType: Int = DeviceType.PHONE.value,
        startTime: Long = 0,
        endTime: Long = 0,
        durationSeconds: Long = 0,
        sleepScore: Int? = null,
        stagesJson: String? = null
    ) {
        initBaseEntity(id, eventId, uuid, received, timestamp, deviceType)
        this.startTime = startTime
        this.endTime = endTime
        this.durationSeconds = durationSeconds
        this.sleepScore = sleepScore
        this.stagesJson = stagesJson
    }

    override fun csvHeader() = "eventId,uuid,received,timestamp,startTime,endTime,durationSeconds,sleepScore,stages"
    override fun toCsvRow(): String {
        val escapedStages = stagesJson?.replace("\"", "\"\"") ?: ""
        return "$eventId,$uuid,$received,$timestamp,$startTime,$endTime,$durationSeconds,$sleepScore,\"$escapedStages\""
    }

    override fun toRecord() = SensorRecord(
        id = id,
        timestamp = timestamp,
        fields = mapOf(
            "Duration" to "${durationSeconds}s",
            "Score" to (sleepScore?.toString() ?: "N/A")
        )
    )
}
