package kaist.iclab.mobiletracker.db.entity

import kaist.iclab.mobiletracker.repository.SensorRecord

interface RecordSerializable {
    fun toRecord(): SensorRecord
}
