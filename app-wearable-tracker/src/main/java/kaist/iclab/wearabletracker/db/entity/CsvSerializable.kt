package kaist.iclab.wearabletracker.db.entity

/**
 * Interface for entities that can be serialized to CSV format.
 * Implement this in each entity to enable generic CSV export.
 */
interface CsvSerializable {
    /**
     * Returns the CSV header row for this entity type.
     */
    fun toCsvHeader(): String

    /**
     * Returns a single CSV row representing this entity's data.
     */
    fun toCsvRow(): String

    /**
     * The timestamp of the record
     */
    val timestamp: Long
}
