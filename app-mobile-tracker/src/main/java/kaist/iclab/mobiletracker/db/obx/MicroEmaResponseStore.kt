package kaist.iclab.mobiletracker.db.obx

import io.objectbox.BoxStore
import kaist.iclab.mobiletracker.db.entity.phone.MicroEmaResponseEntity
import kaist.iclab.mobiletracker.db.entity.phone.MicroEmaResponseEntity_

/**
 * ObjectBox-backed store for locally cached microEMA responses. Replaces the Room
 * `MicroEmaResponseDao`; method names/signatures are kept identical so callers only change type.
 */
class MicroEmaResponseStore(boxStore: BoxStore) {
    private val box = boxStore.boxFor(MicroEmaResponseEntity::class.java)

    suspend fun insertAll(responses: List<MicroEmaResponseEntity>) {
        box.put(responses)
    }

    suspend fun getUnsyncedResponses(): List<MicroEmaResponseEntity> =
        box.query().equal(MicroEmaResponseEntity_.isSynced, false).build().use { it.find() }

    suspend fun markAsSynced(ids: List<Long>) {
        val items = ids.mapNotNull { box.get(it) }
        items.forEach { it.isSynced = true }
        box.put(items)
    }

    suspend fun deleteSyncedResponses() {
        box.query().equal(MicroEmaResponseEntity_.isSynced, true).build().use { it.remove() }
    }
}
