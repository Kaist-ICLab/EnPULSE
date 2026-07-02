package kaist.iclab.mobiletracker.db.obx

import kaist.iclab.tracker.sensor.core.SensorEntity

/**
 * Binds a phone sensor's [SensorStore] to the mapping that turns an incoming library
 * [SensorEntity] into the stored ObjectBox entity. This is the small per-sensor glue that
 * replaced each Room DAO's `insert()`/`insertBatch()` body; the mapping functions themselves
 * live in [PhoneEntityMappers].
 */
class PhoneSensorStore<T : Any>(
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
    fun recordCount(): Int = store.count().toInt()
}
