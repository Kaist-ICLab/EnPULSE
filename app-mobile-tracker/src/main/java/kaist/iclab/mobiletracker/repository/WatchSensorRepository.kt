package kaist.iclab.mobiletracker.repository

import kaist.iclab.mobiletracker.db.entity.BaseEntity
import kotlinx.coroutines.flow.Flow

interface WatchSensorRepository {
    suspend fun insertBatch(sensorId: String, entities: List<BaseEntity>): Result<Unit>


    suspend fun getLatestTimestamp(sensorId: String): Long?
    suspend fun getRecordCount(sensorId: String): Int
    suspend fun deleteAllSensorData(sensorId: String): Result<Unit>
    suspend fun flushAllData(): Result<Unit>

    fun getWatchConnectionInfo(): Flow<WatchConnectionInfo>
}
