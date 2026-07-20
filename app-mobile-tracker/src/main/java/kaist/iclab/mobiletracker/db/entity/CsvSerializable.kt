package kaist.iclab.mobiletracker.db.entity

interface CsvSerializable {
    fun csvHeader(): String
    fun toCsvRow(): String
}
