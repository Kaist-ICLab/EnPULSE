package kaist.iclab.mobiletracker.db.obx

import io.objectbox.BoxStore
import io.objectbox.query.QueryBuilder
import kaist.iclab.mobiletracker.db.entity.phone.WebAppLogEntity
import kaist.iclab.mobiletracker.db.entity.phone.WebAppLogEntity_

/**
 * ObjectBox-backed store for webapp log events. Mirrors [MicroEmaResponseStore] rather than
 * [SensorStore]: webapp logs are not sensor data, so they need neither the `BaseEntity` columns
 * (`event_id`, `device_type`, `received`) nor the dedup strategies, and they are uploaded by
 * [kaist.iclab.mobiletracker.services.upload.WebAppLogUploader] on their own schedule.
 *
 * Upload progress is tracked per row via [WebAppLogEntity.isSynced] rather than by a
 * last-uploaded-timestamp watermark, because `web_app_log` has no primary key to upsert against —
 * a re-upload would insert duplicates.
 *
 * The `recordsAfter`/`countAfter`/`latestTimestamp` family below exists so
 * [kaist.iclab.mobiletracker.repository.handlers.WebAppLogDataHandler] can surface this store on
 * the Data screen like any other sensor.
 */
class WebAppLogStore(boxStore: BoxStore) {
    private val box = boxStore.boxFor(WebAppLogEntity::class.java)

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
            else box.query().build().use { it.property(WebAppLogEntity_.timestamp).max() }
        cacheInitialized = true
    }

    fun insert(entity: WebAppLogEntity): Long = synchronized(cacheLock) {
        ensureCacheInitializedLocked()
        val before = box.count()
        val id = box.put(entity)
        cachedCount += (box.count() - before).toInt()
        if (cachedLatestTimestamp == null || entity.timestamp > cachedLatestTimestamp!!) {
            cachedLatestTimestamp = entity.timestamp
        }
        id
    }

    /** Oldest-first batch of rows not yet accepted by Supabase. */
    fun unsynced(limit: Long): List<WebAppLogEntity> =
        box.query().equal(WebAppLogEntity_.isSynced, false).build()
            .use { it.find(0, limit) }

    fun hasUnsynced(): Boolean =
        box.query().equal(WebAppLogEntity_.isSynced, false).build().use { it.count() > 0 }

    /**
     * Records Supabase has actually accepted, counted directly from [WebAppLogEntity.isSynced]
     * rather than a separately-tracked running total — this table is low-volume (unlike the ~30
     * sensor stores queried on every Data tab load), so a live query here is cheap.
     */
    fun syncedCount(): Long =
        box.query().equal(WebAppLogEntity_.isSynced, true).build().use { it.count() }

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
        box.query().greaterOrEqual(WebAppLogEntity_.timestamp, afterTimestamp)
            .build().use { it.count() }

    fun latestTimestamp(): Long? = synchronized(cacheLock) {
        ensureCacheInitializedLocked()
        cachedLatestTimestamp
    }

    fun recordsAfter(afterTimestamp: Long, isAscending: Boolean, limit: Int, offset: Int): List<WebAppLogEntity> {
        val builder = box.query().greaterOrEqual(WebAppLogEntity_.timestamp, afterTimestamp)
        builder.order(WebAppLogEntity_.timestamp, if (isAscending) 0 else QueryBuilder.DESCENDING)
        return builder.build().use { it.find(offset.toLong(), limit.toLong()) }
    }

    fun removeById(id: Long): Boolean {
        val removed = box.remove(id)
        if (removed) {
            synchronized(cacheLock) {
                ensureCacheInitializedLocked()
                cachedCount = (cachedCount - 1).coerceAtLeast(0)
                cachedLatestTimestamp = if (box.isEmpty) null
                    else box.query().build().use { it.property(WebAppLogEntity_.timestamp).max() }
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
