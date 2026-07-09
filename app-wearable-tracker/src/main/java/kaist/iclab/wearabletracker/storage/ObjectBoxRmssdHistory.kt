package kaist.iclab.wearabletracker.storage

import io.objectbox.BoxStore
import kaist.iclab.tracker.storage.core.RmssdHistory
import kaist.iclab.wearabletracker.db.entity.RmssdHistoryEntity

class ObjectBoxRmssdHistory(boxStore: BoxStore) : RmssdHistory {
    private val box = boxStore.boxFor(RmssdHistoryEntity::class.java)

    override suspend fun insert(timestampMs: Long, rmssd: Float) {
        box.put(RmssdHistoryEntity(timestamp = timestampMs, rmssd = rmssd))
    }

    override suspend fun all(): FloatArray = box.all.map { it.rmssd }.toFloatArray()

    override suspend fun clear() = box.removeAll()
}
