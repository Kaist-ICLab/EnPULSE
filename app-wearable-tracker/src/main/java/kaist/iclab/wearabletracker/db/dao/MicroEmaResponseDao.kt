package kaist.iclab.wearabletracker.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kaist.iclab.wearabletracker.db.entity.MicroEmaResponseEntity

/**
 * Data Access Object for microEMA responses.
 */
@Dao
interface MicroEmaResponseDao {

    @Insert
    suspend fun insertAll(responses: List<MicroEmaResponseEntity>): List<Long>

    @Query("SELECT * FROM micro_ema_responses WHERE synced = 0 ORDER BY triggerTime ASC")
    suspend fun getUnsyncedResponses(): List<MicroEmaResponseEntity>

    @Query("UPDATE micro_ema_responses SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)

    @Query("SELECT COUNT(*) FROM micro_ema_responses")
    suspend fun getTotalCount(): Int

    @Query("SELECT COUNT(*) FROM micro_ema_responses WHERE synced = 0")
    suspend fun getUnsyncedCount(): Int
}
