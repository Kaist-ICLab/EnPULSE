package kaist.iclab.wearabletracker.storage

import kaist.iclab.tracker.storage.core.RmssdHistory
import kaist.iclab.wearabletracker.db.dao.RmssdHistoryDao
import kaist.iclab.wearabletracker.db.entity.RmssdHistoryEntity

class RoomRmssdHistory(
    private val dao: RmssdHistoryDao,
) : RmssdHistory {
    override suspend fun insert(timestampMs: Long, rmssd: Float) {
        dao.insert(RmssdHistoryEntity(timestamp = timestampMs, rmssd = rmssd))
    }

    override suspend fun all(): FloatArray {
        val list = dao.getAllRmssds()
        val out = FloatArray(list.size)
        for (i in list.indices) out[i] = list[i]
        return out
    }

    override suspend fun clear() {
        dao.deleteAll()
    }
}
