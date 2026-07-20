package kaist.iclab.wearabletracker.db.entity

import io.objectbox.annotation.Entity

@Entity
class GestureEntity : WatchBaseEntity, CsvSerializable {
    var classIndex: Int = 0
    var score: Int = 0
    var probabilities: IntArray = IntArray(0)

    constructor() : super()

    constructor(
        received: Long,
        timestamp: Long,
        classIndex: Int,
        score: Int,
        probabilities: IntArray
    ) : super() {
        initBase(received, timestamp)
        this.classIndex = classIndex
        this.score = score
        this.probabilities = probabilities
    }

    override fun csvHeader(): String = "eventId,received,timestamp,classIndex,score,probabilities"
    override fun toCsvRow(): String =
        "$eventId,$received,$timestamp,$classIndex,$score,${probabilities.joinToString(";")}"
}
