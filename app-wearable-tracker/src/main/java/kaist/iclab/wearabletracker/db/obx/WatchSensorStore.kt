package kaist.iclab.wearabletracker.db.obx

import io.objectbox.Box
import io.objectbox.BoxStore
import io.objectbox.Property
import kaist.iclab.tracker.sensor.core.SensorEntity
import kaist.iclab.wearabletracker.db.entity.CsvSerializable
import kaist.iclab.wearabletracker.db.entity.WatchBaseEntity

class WatchSensorStore<T>(
    boxStore: BoxStore,
    entityClass: Class<T>,
    private val fromSensorEntity: (SensorEntity) -> List<T>
) where T : WatchBaseEntity, T : CsvSerializable {

    private val box: Box<T> = boxStore.boxFor(entityClass)
    private val metaClass = Class.forName("${entityClass.name}_")

    @Suppress("UNCHECKED_CAST")
    private val timestampProperty = metaClass.getField("timestamp").get(null) as Property<T>

    fun insertFromSensorEntities(entities: List<SensorEntity>) {
        box.put(entities.flatMap { fromSensorEntity(it) })
    }

    fun getAllForExport(): List<CsvSerializable> = box.all

    /**
     * Up to [limit] rows after [timestamp], extended so the batch never ends partway through rows
     * that share one timestamp: the next batch resumes strictly after this batch's last timestamp,
     * and the ACK deletes everything at or before it, so a split group would be skipped and deleted.
     */
    fun getDataSince(timestamp: Long, limit: Int): List<CsvSerializable> {
        val batch = box.query(timestampProperty.greater(timestamp))
            .order(timestampProperty)
            .build()
            .use { it.find(0, limit.toLong()) }
        if (batch.size < limit) return batch

        val lastTimestamp = batch.last().timestamp
        val lastGroup = box.query(timestampProperty.equal(lastTimestamp)).build().use { it.find() }
        return batch.filter { it.timestamp < lastTimestamp } + lastGroup
    }

    fun deleteDataBefore(timestamp: Long) =
        box.query(timestampProperty.lessOrEqual(timestamp)).build().use { it.remove() }

    fun deleteAll() = box.removeAll()

    fun getCount(): Int = box.count().toInt()

    fun getCountSince(timestamp: Long): Int =
        box.query(timestampProperty.greater(timestamp)).build().use { it.count().toInt() }
}
