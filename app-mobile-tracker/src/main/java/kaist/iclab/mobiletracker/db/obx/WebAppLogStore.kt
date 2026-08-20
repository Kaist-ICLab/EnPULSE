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

    fun insert(entity: WebAppLogEntity): Long = box.put(entity)

    /** Oldest-first batch of rows not yet accepted by Supabase. */
    fun unsynced(limit: Long): List<WebAppLogEntity> =
        box.query().equal(WebAppLogEntity_.isSynced, false).build()
            .use { it.find(0, limit) }

    fun hasUnsynced(): Boolean =
        box.query().equal(WebAppLogEntity_.isSynced, false).build().use { it.count() > 0 }

    fun markSynced(ids: List<Long>) {
        val items = ids.mapNotNull { box.get(it) }
        items.forEach { it.isSynced = true }
        box.put(items)
    }

    fun count(): Long = box.count()

    fun countAfter(afterTimestamp: Long): Long =
        box.query().greaterOrEqual(WebAppLogEntity_.timestamp, afterTimestamp)
            .build().use { it.count() }

    fun latestTimestamp(): Long? {
        if (box.isEmpty) return null
        return box.query().build().use { it.property(WebAppLogEntity_.timestamp).max() }
    }

    fun recordsAfter(
        afterTimestamp: Long,
        isAscending: Boolean,
        limit: Int,
        offset: Int
    ): List<WebAppLogEntity> {
        val builder = box.query().greaterOrEqual(WebAppLogEntity_.timestamp, afterTimestamp)
        builder.order(WebAppLogEntity_.timestamp, if (isAscending) 0 else QueryBuilder.DESCENDING)
        return builder.build().use { it.find(offset.toLong(), limit.toLong()) }
    }

    fun removeById(id: Long): Boolean = box.remove(id)

    fun removeAll() = box.removeAll()
}
