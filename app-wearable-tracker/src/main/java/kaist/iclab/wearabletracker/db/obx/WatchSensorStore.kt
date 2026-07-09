package kaist.iclab.wearabletracker.db.obx

import io.objectbox.Box
import io.objectbox.BoxStore
import io.objectbox.Property
import io.objectbox.query.QueryBuilder
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

    fun getDataSince(timestamp: Long, limit: Int): List<CsvSerializable> =
        box.query(timestampProperty.greater(timestamp))
            .order(timestampProperty)
            .build()
            .use { it.find(0, limit.toLong()) }

    fun deleteDataBefore(timestamp: Long) =
        box.query(timestampProperty.lessOrEqual(timestamp)).build().use { it.remove() }

    fun deleteAll() = box.removeAll()

    fun getCount(): Int = box.count().toInt()

    fun getCountSince(timestamp: Long): Int =
        box.query(timestampProperty.greater(timestamp)).build().use { it.count().toInt() }
}
