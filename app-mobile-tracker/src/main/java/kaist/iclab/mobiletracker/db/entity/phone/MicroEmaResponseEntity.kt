package kaist.iclab.mobiletracker.db.entity.phone

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for locally caching microEMA survey responses on the phone.
 * Allows the phone to immediately ACK the watch and upload to Supabase asynchronously.
 */
@Entity(tableName = "micro_ema_responses_phone")
data class MicroEmaResponseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** The ID of the response in the watch's local database (for ACK tracking) */
    val watchId: Long,

    val uuid: String,
    val questionId: Int,
    val triggerTime: String?,
    val actualTriggerTime: String?,
    val surveyStartTime: String?,
    val responseSubmissionTime: String?,

    /** The JSON string of the response payload (value and status) */
    val responseJson: String,

    val isSynced: Boolean = false
)
