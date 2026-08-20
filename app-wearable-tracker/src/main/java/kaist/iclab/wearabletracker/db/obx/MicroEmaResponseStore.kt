package kaist.iclab.wearabletracker.db.obx

import io.objectbox.BoxStore
import kaist.iclab.wearabletracker.db.entity.MicroEmaResponseEntity
import kaist.iclab.wearabletracker.db.entity.MicroEmaResponseEntity_

class MicroEmaResponseStore(boxStore: BoxStore) {
    private val box = boxStore.boxFor(MicroEmaResponseEntity::class.java)

    fun insertAll(entities: List<MicroEmaResponseEntity>): List<Long> {
        box.put(entities)
        return entities.map { it.id }
    }

    fun getUnsyncedResponses(): List<MicroEmaResponseEntity> =
        box.query().equal(MicroEmaResponseEntity_.isSynced, false).build().use { it.find() }

    fun markSynced(ids: List<Long>) {
        val items = ids.mapNotNull { box.get(it) }
        items.forEach { it.isSynced = true }
        box.put(items)
    }

    fun getTotalCount(): Int = box.count().toInt()

    fun getUnsyncedCount(): Int =
        box.query().equal(MicroEmaResponseEntity_.isSynced, false).build()
            .use { it.count().toInt() }
}
