package kaist.iclab.mobiletracker.db.entity.phone

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index
import kaist.iclab.mobiletracker.data.DeviceType

/**
 * Legacy: how watch microEMA answers were cached before they became [SurveyResponseEntity] rows.
 * Only read by [kaist.iclab.mobiletracker.db.obx.MicroEmaResponseStore.migrateInto]; kept so an
 * upgrade doesn't drop answers that hadn't been uploaded yet.
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
