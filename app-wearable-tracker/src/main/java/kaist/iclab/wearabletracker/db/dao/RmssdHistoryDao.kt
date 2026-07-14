package kaist.iclab.wearabletracker.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kaist.iclab.wearabletracker.db.entity.RmssdHistoryEntity

@Dao
interface RmssdHistoryDao {
    @Insert
    suspend fun insert(entity: RmssdHistoryEntity)

    @Query("SELECT rmssd FROM RmssdHistoryEntity ORDER BY timestamp ASC")
    suspend fun getAllRmssds(): List<Float>

    @Query("DELETE FROM RmssdHistoryEntity")
    suspend fun deleteAll()
}
