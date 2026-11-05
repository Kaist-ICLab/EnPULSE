package kaist.iclab.mobiletracker.db.entity.phone

import io.objectbox.annotation.Entity
import kaist.iclab.mobiletracker.data.DeviceType
import kaist.iclab.mobiletracker.db.entity.BaseEntity
import kaist.iclab.mobiletracker.db.entity.CsvSerializable
import kaist.iclab.mobiletracker.db.entity.RecordSerializable
import kaist.iclab.mobiletracker.repository.SensorRecord
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Entity
@Serializable
class ActivityRecognitionEntity : BaseEntity, CsvSerializable, RecordSerializable {
    @SerialName("elapsed_realtime_millis")
    var elapsedRealtimeMillis: Long = 0

    @SerialName("activity_type")
    var activityType: Int = 0
    var score: Int = 0
    var probabilities: IntArray = IntArray(0)

    constructor() : super()

    constructor(
        id: Long = 0,
        eventId: String = UUID.randomUUID().toString(),
        uuid: String = "",
        received: Long = 0,
        timestamp: Long = 0,
        deviceType: Int = DeviceType.PHONE.value,
        elapsedRealtimeMillis: Long = 0,
        activityType: Int = 0,
        score: Int = 0,
        probabilities: IntArray = IntArray(0)
    ) {
        initBaseEntity(id, eventId, uuid, received, timestamp, deviceType)
        this.elapsedRealtimeMillis = elapsedRealtimeMillis
        this.activityType = activityType
        this.score = score
        this.probabilities = probabilities
    }

    override fun csvHeader() =
        "eventId,uuid,received,timestamp,elapsedRealtimeMillis,activityType,score,probabilities"

    override fun toCsvRow(): String {
        val escapedProbs = probabilities.joinToString("|").replace("\"", "\"\"")
        return "$eventId,$uuid,$received,$timestamp,$elapsedRealtimeMillis,$activityType,$score,\"$escapedProbs\""
    }

    override fun toRecord() = SensorRecord(
        id = id,
        timestamp = timestamp,
        fields = mapOf("Activity" to activityType.toString(), "Score" to score.toString())
    )
}
