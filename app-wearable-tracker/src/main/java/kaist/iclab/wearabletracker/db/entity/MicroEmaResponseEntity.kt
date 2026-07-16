package kaist.iclab.wearabletracker.db.entity

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index

@Entity
data class MicroEmaResponseEntity(
    @Id var id: Long = 0,
    var surveyId: Int = 0,
    var questionId: Int = 0,
    var answer: String? = null,
    var status: String = "",
    var triggerTime: Long = 0,
    var surveyStartTime: Long = 0,
    var responseTime: Long? = null,
    @Index var isSynced: Boolean = false
)
