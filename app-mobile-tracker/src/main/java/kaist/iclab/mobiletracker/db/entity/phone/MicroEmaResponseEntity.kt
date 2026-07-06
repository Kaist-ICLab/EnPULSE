package kaist.iclab.mobiletracker.db.entity.phone

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index
import kaist.iclab.mobiletracker.data.DeviceType

/**
 * ObjectBox entity for locally caching microEMA survey responses on the phone.
 * Allows the phone to immediately ACK the watch and upload to Supabase asynchronously.
 *
 * This is not a sensor and does not go through [SensorStore]; it is accessed via a small
 * dedicated store that queries on [isSynced].
 */
@Entity
data class MicroEmaResponseEntity(
    @Id var id: Long = 0,

    /** The ID of the response in the watch's local database (for ACK tracking) */
    var watchId: Long = 0,

    var uuid: String = "",
    var questionId: Int = 0,
    var triggerTime: String? = null,
    var actualTriggerTime: String? = null,
    var surveyStartTime: String? = null,
    var responseSubmissionTime: String? = null,

    /** The JSON string of the response payload (value and status) */
    var responseJson: String = "",

    var deviceType: Int = DeviceType.WATCH.value,

    @Index var isSynced: Boolean = false
)
