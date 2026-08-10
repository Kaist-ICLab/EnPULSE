package kaist.iclab.mobiletracker.db.obx

import io.objectbox.BoxStore
import io.objectbox.query.QueryBuilder
import kaist.iclab.mobiletracker.db.entity.phone.SurveyResponseEntity
import kaist.iclab.mobiletracker.db.entity.phone.SurveyResponseEntity_

/**
 * ObjectBox-backed store for locally cached phone survey responses. Mirrors [WebAppLogStore]:
 * not a sensor, so it lives outside [SensorStores], and upload progress is tracked per row via
 * [SurveyResponseEntity.isSynced] rather than a timestamp watermark, since
 * `survey_question_response` has no local-friendly key to upsert against.
 *
 * The `recordsAfter`/`countAfter`/`latestTimestamp` family below exists so
 * [kaist.iclab.mobiletracker.repository.handlers.SurveyResponseDataHandler] can surface this store
 * on the Data screen like any other sensor; they order/filter on [SurveyResponseEntity.responseSubmissionTime]
 * as the closest analog to a sensor's `timestamp` column.
 */
class SurveyResponseStore(boxStore: BoxStore) {
    private val box = boxStore.boxFor(SurveyResponseEntity::class.java)

    fun insertAll(entities: List<SurveyResponseEntity>) {
        box.put(entities)
    }

    /** Oldest-first batch of rows not yet accepted by Supabase. */
    fun unsynced(limit: Long): List<SurveyResponseEntity> =
        box.query().equal(SurveyResponseEntity_.isSynced, false).build()
            .use { it.find(0, limit) }

    fun hasUnsynced(): Boolean =
        box.query().equal(SurveyResponseEntity_.isSynced, false).build().use { it.count() > 0 }

    fun markSynced(ids: List<Long>) {
        val items = ids.mapNotNull { box.get(it) }
        items.forEach { it.isSynced = true }
        box.put(items)
    }

    fun count(): Long = box.count()

    fun countAfter(afterTimestamp: Long): Long =
        box.query().greaterOrEqual(SurveyResponseEntity_.responseSubmissionTime, afterTimestamp)
            .build().use { it.count() }

    fun latestTimestamp(): Long? {
        if (box.isEmpty) return null
        return box.query().build().use { it.property(SurveyResponseEntity_.responseSubmissionTime).max() }
    }

    fun recordsAfter(afterTimestamp: Long, isAscending: Boolean, limit: Int, offset: Int): List<SurveyResponseEntity> {
        val builder = box.query().greaterOrEqual(SurveyResponseEntity_.responseSubmissionTime, afterTimestamp)
        builder.order(SurveyResponseEntity_.responseSubmissionTime, if (isAscending) 0 else QueryBuilder.DESCENDING)
        return builder.build().use { it.find(offset.toLong(), limit.toLong()) }
    }

    fun removeById(id: Long): Boolean = box.remove(id)

    fun removeAll() = box.removeAll()
}
