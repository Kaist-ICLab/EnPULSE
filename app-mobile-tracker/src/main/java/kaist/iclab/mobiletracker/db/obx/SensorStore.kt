package kaist.iclab.mobiletracker.db.obx

import io.objectbox.Box
import io.objectbox.BoxStore
import io.objectbox.Property
import io.objectbox.query.QueryBuilder
import kaist.iclab.mobiletracker.db.entity.BaseEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn

/**
 * A single generic replacement for the entire per-sensor Room DAO layer.
 *
 * ObjectBox's `Box<T>` and `Property<T>` are generic, so — unlike Room, where `@Query` needs a
 * literal table name and a concrete return type — one class can serve every sensor. Each sensor is
 * "registered" by handing this store its entity class plus the two properties it queries on
 * (`timestamp`, `deviceType`), taken from ObjectBox's generated `Entity_` metadata. That few-line
 * registration is all that remains of what used to be a full DAO per sensor.
 *
 * Deletes go through the Box, and the reactive count uses ObjectBox's own change notifications, so
 * observers (the home-screen daily counts) still update on mutation.
 */
open class SensorStore<T : BaseEntity>(
    protected val boxStore: BoxStore,
    private val entityClass: Class<T>,
    /**
     * When true, insert/insertBatch upsert by (timestamp, deviceType) instead of always inserting
     * a new row. Needed for sensors (Sleep/Step/Exercise) whose source re-reads a trailing margin
     * of already-seen data on every poll — without this they'd accumulate duplicate rows for the
     * same session/bucket.
     */
    private val dedupByTimestamp: Boolean = false,
) {
    protected val box: Box<T> = boxStore.boxFor(entityClass)

    private val metaClass = Class.forName("${entityClass.name}_")
    @Suppress("UNCHECKED_CAST")
    private val timestampProperty = metaClass.getField("timestamp").get(null) as Property<T>
    @Suppress("UNCHECKED_CAST")
    private val deviceTypeProperty = metaClass.getField("deviceType").get(null) as Property<T>

    fun insert(entity: T): Long {
        if (dedupByTimestamp) applyExistingIds(listOf(entity))
        return box.put(entity)
    }

    fun insertBatch(entities: List<T>) {
        if (dedupByTimestamp) applyExistingIds(entities)
        box.put(entities)
    }

    /**
     * For dedup-enabled stores, mutates [entities] in place so any entity matching an existing
     * row's (timestamp, deviceType) reuses that row's id — making the subsequent `box.put` an
     * update instead of an insert.
     */
    private fun applyExistingIds(entities: List<T>) {
        if (entities.isEmpty()) return
        val timestamps = entities.map { it.timestamp }.distinct().toLongArray()
        val existingByKey = box.query(timestampProperty.oneOf(timestamps))
            .build()
            .use { it.find() }
            .associate { (it.timestamp to it.deviceType) to it.id }
        entities.forEach { entity ->
            existingByKey[entity.timestamp to entity.deviceType]?.let { existingId ->
                entity.id = existingId
            }
        }
    }

    fun count(deviceType: Int? = null): Int = box.query().run {
        if (deviceType != null) {
            equal(deviceTypeProperty, deviceType.toLong())
        } else {
            this
        }
    }.build().use { it.count().toInt() }

    fun countAfter(afterTimestamp: Long, deviceType: Int? = null): Long =
        box.query().run {
            if (deviceType != null) equal(deviceTypeProperty, deviceType.toLong()) else this
        }
            .greaterOrEqual(timestampProperty, afterTimestamp).build()
            .use { it.count() }

    fun hasDataAfter(afterTimestamp: Long, deviceType: Int? = null): Boolean =
        box.query().run {
            if (deviceType != null) equal(deviceTypeProperty, deviceType.toLong()) else this
        }
            .greater(timestampProperty, afterTimestamp).build()
            .use { it.count() > 0 }

    fun latestTimestamp(deviceType: Int? = null): Long? {
        if (box.isEmpty) return null
        return box.query().run {
            if (deviceType != null) equal(deviceTypeProperty, deviceType.toLong()) else this
        }.build().use { it.property(timestampProperty).max() }
    }

    fun recordsAfter(
        afterTimestamp: Long,
        isAscending: Boolean,
        limit: Int,
        offset: Int,
        deviceType: Int? = null
    ): List<T> {
        val builder = box.query().greaterOrEqual(timestampProperty, afterTimestamp).run {
            if (deviceType != null) equal(deviceTypeProperty, deviceType.toLong()) else this
        }
        builder.order(timestampProperty, if (isAscending) 0 else QueryBuilder.DESCENDING)
        return builder.build().use { it.find(offset.toLong(), limit.toLong()) }
    }

    fun newInstance(): T = entityClass.getDeclaredConstructor().newInstance()

    fun eventIdById(id: Long): String? = box.get(id)?.eventId

    fun removeById(id: Long): Boolean = box.remove(id)

    fun removeBefore(timestamp: Long, deviceType: Int? = null): Long =
        box.query().run {
            if (deviceType != null) equal(deviceTypeProperty, deviceType.toLong()) else this
        }.less(timestampProperty, timestamp).build().use { it.remove() }

    fun removeAll() = box.removeAll()

    /**
     * Emits the count of records at/after [afterTimestamp] and re-emits whenever this entity's data
     * changes. Replaces the per-DAO `getDaily<X>Count(): Flow<Int>` Room queries.
     */
    fun countAfterFlow(afterTimestamp: Long, deviceType: Int? = null): Flow<Int> = callbackFlow {
        fun current(): Int = countAfter(afterTimestamp, deviceType).toInt()
        trySend(current())
        val subscription = boxStore.subscribe(entityClass).observer { _ -> trySend(current()) }
        awaitClose { subscription.cancel() }
    }.flowOn(Dispatchers.IO).conflate()
}
