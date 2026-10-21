package kaist.iclab.mobiletracker.db.obx

import io.objectbox.BoxStore
import kaist.iclab.mobiletracker.data.DeviceType
import kaist.iclab.mobiletracker.db.entity.phone.MicroEmaResponseEntity
import kaist.iclab.mobiletracker.db.entity.phone.MicroEmaResponseEntity_
import kaist.iclab.mobiletracker.db.entity.phone.SurveyResponseEntity

/**
 * Legacy store for watch MicroEMA answers, which are now rows of the "Survey" sensor
 * ([SensorStores.surveyResponse]). Kept only so answers queued by an older build still get
 * uploaded: removing [MicroEmaResponseEntity] would drop its box, unsynced rows included.
 */
class MicroEmaResponseStore(boxStore: BoxStore) {
    private val box = boxStore.boxFor(MicroEmaResponseEntity::class.java)

    /**
     * Moves every not-yet-uploaded answer into [target] and empties this box. Rows already
     * uploaded are dropped. Uses the same eventId as BLEHelper, so a watch resend of a migrated
     * answer is still recognized as a duplicate.
     *
     * @return how many answers were moved.
     */
    fun migrateInto(target: SensorStore<SurveyResponseEntity>): Int {
        if (box.isEmpty) return 0
        val unsynced = box.query().equal(MicroEmaResponseEntity_.isSynced, false).build()
            .use { it.find() }
        val moved = target.insertBatch(unsynced.map { it.toSurveyResponse() })
        box.removeAll()
        return moved
    }

    private fun MicroEmaResponseEntity.toSurveyResponse(): SurveyResponseEntity {
        val now = System.currentTimeMillis()
        val triggerMillis = triggerTime?.toLongOrNull() ?: now
        return SurveyResponseEntity(
            eventId = "microema-$questionId-$triggerTime-$surveyStartTime",
            deviceType = DeviceType.WATCH.value,
            questionId = questionId,
            triggerTime = triggerMillis,
            actualTriggerTime = actualTriggerTime?.toLongOrNull() ?: triggerMillis,
            surveyStartTime = surveyStartTime?.toLongOrNull() ?: now,
            responseSubmissionTime = responseSubmissionTime?.toLongOrNull() ?: triggerMillis,
            responseJson = responseJson
        )
    }
}
