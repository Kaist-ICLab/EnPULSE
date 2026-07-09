package kaist.iclab.wearabletracker.db.entity

import io.objectbox.annotation.Entity

@Entity
class HeartRateEntity() : WatchBaseEntity(), CsvSerializable {
    var hr: Int = 0
    var hrStatus: Int = 0
    var ibi: IntArray = IntArray(0)
    var ibiStatus: IntArray = IntArray(0)

    constructor(
        received: Long, timestamp: Long,
        hr: Int, hrStatus: Int,
        ibi: IntArray, ibiStatus: IntArray
    ) : this() {
        initBase(received, timestamp)
        this.hr = hr; this.hrStatus = hrStatus
        this.ibi = ibi; this.ibiStatus = ibiStatus
    }

    override fun toCsvHeader() = "eventId,received,timestamp,hr,hrStatus,ibi,ibiStatus"
    override fun toCsvRow(): String {
        val ibiStr = ibi.joinToString(";")
        val ibiStatusStr = ibiStatus.joinToString(";")
        return "$eventId,$received,$timestamp,$hr,$hrStatus,$ibiStr,$ibiStatusStr"
    }
}
