package kaist.iclab.wearabletracker.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for locally caching microEMA survey responses on the watch.
 * Responses are persisted here first, then synced to the phone via BLE.
 *
 * Maps to the Supabase `survey_question_response` table structure.
 */
@Entity(tableName = "micro_ema_responses")
data class MicroEmaResponseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Which survey this response belongs to */
    val surveyId: Int,

    /** Which question was answered (maps to survey_question.id) */
    val questionId: Int,

    /** The answer value (option display text, number, etc.). Null if EXPIRED or DISMISSED. */
    val answer: String?,

    /** Compliance status: "ANSWERED", "EXPIRED", or "DISMISSED" */
    val status: String,

    /** When the EMA was triggered (button press timestamp) */
    val triggerTime: Long,

    /** When the first question was shown to the user */
    val surveyStartTime: Long,

    /** When this specific question was answered. Null if not answered. */
    val responseTime: Long?,

    /** Whether this response has been synced to the phone */
    val synced: Boolean = false
)
