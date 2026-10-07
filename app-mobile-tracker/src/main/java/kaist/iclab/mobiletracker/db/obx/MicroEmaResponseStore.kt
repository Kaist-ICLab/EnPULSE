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

    /**
     * Inserts only the responses not already stored, and returns how many were new. The watch
     * re-sends answers until it gets the phone's ACK, so after a lost ACK or a reconnect the same
     * answer arrives again. One answer is identified by its question and its trigger and start
     * times (synced rows are kept, so this also catches one that was already uploaded).
     */
    fun insertNew(responses: List<MicroEmaResponseEntity>): Int = synchronized(this) {
        val fresh = responses
            .distinctBy { it.dedupeKey() }
            .filter { response ->
                box.query()
                    .equal(MicroEmaResponseEntity_.questionId, response.questionId.toLong())
                    .build()
                    .use { query -> query.find() }
                    .none { it.dedupeKey() == response.dedupeKey() }
            }
        if (fresh.isNotEmpty()) box.put(fresh)
        fresh.size
    }

    private fun MicroEmaResponseEntity.dedupeKey() =
        Triple(questionId, triggerTime, surveyStartTime)

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
