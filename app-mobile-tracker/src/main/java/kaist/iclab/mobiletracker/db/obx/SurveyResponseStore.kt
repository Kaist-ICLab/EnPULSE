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

    // See SensorStore's class doc for why: count()/latestTimestamp() are read on every Data tab
    // load, so they're backed by a running total instead of a live query, adjusted on each
    // mutation rather than recomputed on every read.
    private val cacheLock = Any()
    private var cacheInitialized = false
    private var cachedCount = 0
    private var cachedLatestTimestamp: Long? = null

    private fun ensureCacheInitializedLocked() {
        if (cacheInitialized) return
        cachedCount = box.count().toInt()
        cachedLatestTimestamp = if (box.isEmpty) null
            else box.query().build().use { it.property(SurveyResponseEntity_.responseSubmissionTime).max() }
        cacheInitialized = true
    }

    fun insertAll(entities: List<SurveyResponseEntity>) {
        synchronized(cacheLock) {
            ensureCacheInitializedLocked()
            val before = box.count()
            box.put(entities)
            cachedCount += (box.count() - before).toInt()
            val maxTimestamp = entities.maxOfOrNull { it.responseSubmissionTime }
            if (maxTimestamp != null &&
                (cachedLatestTimestamp == null || maxTimestamp > cachedLatestTimestamp!!)
            ) {
                cachedLatestTimestamp = maxTimestamp
            }
        }
    }

    /** Oldest-first batch of rows not yet accepted by Supabase. */
    fun unsynced(limit: Long): List<SurveyResponseEntity> =
        box.query().equal(SurveyResponseEntity_.isSynced, false).build()
            .use { it.find(0, limit) }

    fun hasUnsynced(): Boolean =
        box.query().equal(SurveyResponseEntity_.isSynced, false).build().use { it.count() > 0 }

    /**
     * Records Supabase has actually accepted, counted directly from [SurveyResponseEntity.isSynced]
     * rather than a separately-tracked running total — this table is low-volume (unlike the ~30
     * sensor stores queried on every Data tab load), so a live query here is cheap.
     */
    fun syncedCount(): Long =
        box.query().equal(SurveyResponseEntity_.isSynced, true).build().use { it.count() }

    fun markSynced(ids: List<Long>) {
        val items = ids.mapNotNull { box.get(it) }
        items.forEach { it.isSynced = true }
        box.put(items)
    }

    fun count(): Long = synchronized(cacheLock) {
        ensureCacheInitializedLocked()
        cachedCount.toLong()
    }

    fun countAfter(afterTimestamp: Long): Long =
        box.query().greaterOrEqual(SurveyResponseEntity_.responseSubmissionTime, afterTimestamp)
            .build().use { it.count() }

    fun latestTimestamp(): Long? = synchronized(cacheLock) {
        ensureCacheInitializedLocked()
        cachedLatestTimestamp
    }

    fun recordsAfter(afterTimestamp: Long, isAscending: Boolean, limit: Int, offset: Int): List<SurveyResponseEntity> {
        val builder = box.query().greaterOrEqual(SurveyResponseEntity_.responseSubmissionTime, afterTimestamp)
        builder.order(SurveyResponseEntity_.responseSubmissionTime, if (isAscending) 0 else QueryBuilder.DESCENDING)
        return builder.build().use { it.find(offset.toLong(), limit.toLong()) }
    }

    fun removeById(id: Long): Boolean {
        val removed = box.remove(id)
        if (removed) {
            synchronized(cacheLock) {
                ensureCacheInitializedLocked()
                cachedCount = (cachedCount - 1).coerceAtLeast(0)
                cachedLatestTimestamp = if (box.isEmpty) null
                    else box.query().build().use { it.property(SurveyResponseEntity_.responseSubmissionTime).max() }
            }
        }
        return removed
    }

    fun removeAll() {
        box.removeAll()
        synchronized(cacheLock) {
            cachedCount = 0
            cachedLatestTimestamp = null
            cacheInitialized = true
        }
    }
}
