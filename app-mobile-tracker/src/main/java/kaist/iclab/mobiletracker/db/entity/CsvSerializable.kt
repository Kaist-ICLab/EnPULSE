package kaist.iclab.mobiletracker.db.entity

interface CsvSerializable {
    val csvHeader: String
    fun toCsvRow(): String
    val timestamp: Long
}
