package kaist.iclab.mobiletracker.db.obx

import kaist.iclab.mobiletracker.db.entity.BaseEntity
import kaist.iclab.tracker.sensor.core.SensorEntity

/**
 * Binds a phone sensor's [SensorStore] to the mapping that turns an incoming library
 * [SensorEntity] into the stored ObjectBox entity.
 */
class PhoneSensorStore<T : BaseEntity>(
    val store: SensorStore<T>,
    private val fromSensorEntity: (SensorEntity, String?) -> T
) {
    fun insert(entity: SensorEntity, userUuid: String?) {
        store.insert(fromSensorEntity(entity, userUuid))
    }

    fun insertBatch(entities: List<SensorEntity>, userUuid: String?) {
        store.insertBatch(entities.map { fromSensorEntity(it, userUuid) })
    }

    fun deleteAll() = store.removeAll()
    fun latestTimestamp(): Long? = store.latestTimestamp()
    fun recordCount(): Int = store.count()
}
