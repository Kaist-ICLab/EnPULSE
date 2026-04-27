package kaist.iclab.mobiletracker.db.dao.phone


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kaist.iclab.mobiletracker.db.entity.phone.MicroEmaResponseEntity

@Dao
interface MicroEmaResponseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(responses: List<MicroEmaResponseEntity>)

    @Query("SELECT * FROM micro_ema_responses_phone WHERE isSynced = 0")
    suspend fun getUnsyncedResponses(): List<MicroEmaResponseEntity>

    @Query("UPDATE micro_ema_responses_phone SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>)

    @Query("DELETE FROM micro_ema_responses_phone WHERE isSynced = 1")
    suspend fun deleteSyncedResponses()
}
