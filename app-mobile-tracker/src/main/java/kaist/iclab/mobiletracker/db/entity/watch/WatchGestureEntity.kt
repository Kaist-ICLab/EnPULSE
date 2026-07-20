package kaist.iclab.mobiletracker.db.entity.watch

import io.objectbox.annotation.Entity
import kaist.iclab.mobiletracker.data.DeviceType
import kaist.iclab.mobiletracker.db.entity.BaseEntity
import kaist.iclab.mobiletracker.db.entity.CsvSerializable
import kaist.iclab.mobiletracker.db.entity.RecordSerializable
import kaist.iclab.mobiletracker.repository.SensorRecord
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Entity
@Serializable
class WatchGestureEntity : BaseEntity, CsvSerializable, RecordSerializable {
    @SerialName("class_index")
    var classIndex: Int = 0
    var score: Int = 0
    var probabilities: IntArray = IntArray(0)

    constructor() : super()

    constructor(
        id: Long = 0,
        eventId: String = "",
        uuid: String = "",
        received: Long = 0,
        timestamp: Long = 0,
        deviceType: Int = DeviceType.WATCH.value,
        classIndex: Int = 0,
        score: Int = 0,
        probabilities: IntArray = IntArray(0)
    ) {
        initBaseEntity(id, eventId, uuid, received, timestamp, deviceType)
        this.classIndex = classIndex
        this.score = score
        this.probabilities = probabilities
    }

    override fun csvHeader() = "eventId,uuid,received,timestamp,classIndex,score,probabilities"
    override fun toCsvRow(): String {
        val escapedProbs = probabilities.joinToString(";").replace("\"", "\"\"")
        return "$eventId,$uuid,$received,$timestamp,$classIndex,$score,\"$escapedProbs\""
    }

    override fun toRecord() = SensorRecord(
        id = id,
        timestamp = timestamp,
        fields = mapOf(
            "Class Index" to classIndex.toString(),
            "Score" to score.toString(),
            "Probabilities" to probabilities.joinToString(", ")
        )
    )
}
