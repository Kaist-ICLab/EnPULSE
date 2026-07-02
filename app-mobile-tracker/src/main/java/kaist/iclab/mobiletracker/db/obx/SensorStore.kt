package kaist.iclab.mobiletracker.db.obx

import io.objectbox.Box
import io.objectbox.BoxStore
import io.objectbox.Property
import io.objectbox.query.QueryBuilder
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
 * "registered" by handing this store its entity class plus the three properties it queries on
 * (`id`, `timestamp`, `eventId`), taken from ObjectBox's generated `Entity_` metadata. That few-line
 * registration is all that remains of what used to be a full DAO per sensor.
 *
 * Deletes go through the Box, and the reactive count uses ObjectBox's own change notifications, so
 * observers (the home-screen daily counts) still update on mutation.
 */
open class SensorStore<T : Any>(
    protected val boxStore: BoxStore,
    private val entityClass: Class<T>,
    private val idProperty: Property<T>,
    protected val timestampProperty: Property<T>,
    private val eventIdProperty: Property<T>
) {
    protected val box: Box<T> = boxStore.boxFor(entityClass)

    fun insert(entity: T): Long = box.put(entity)

    fun insertBatch(entities: List<T>) = box.put(entities)

    fun count(): Long = box.count()

    fun countAfter(afterTimestamp: Long): Long =
        box.query().greaterOrEqual(timestampProperty, afterTimestamp).build()
            .use { it.count() }

    fun hasDataAfter(afterTimestamp: Long): Boolean =
        box.query().greater(timestampProperty, afterTimestamp).build()
            .use { it.count() > 0 }

    fun latestTimestamp(): Long? {
        if (box.isEmpty) return null
        return box.query().build().use { it.property(timestampProperty).max() }
    }

    fun recordsAfter(
        afterTimestamp: Long,
        isAscending: Boolean,
        limit: Int,
        offset: Int
    ): List<T> {
        val builder = box.query().greaterOrEqual(timestampProperty, afterTimestamp)
        builder.order(timestampProperty, if (isAscending) 0 else QueryBuilder.DESCENDING)
        return builder.build().use { it.find(offset.toLong(), limit.toLong()) }
    }

    fun eventIdById(id: Long): String? =
        box.query().equal(idProperty, id).build()
            .use { it.property(eventIdProperty).findStrings().firstOrNull() }

    fun removeById(id: Long): Boolean = box.remove(id)

    fun removeBefore(timestamp: Long): Long =
        box.query().less(timestampProperty, timestamp).build().use { it.remove() }

    fun removeAll() = box.removeAll()

    /**
     * Emits the count of records at/after [afterTimestamp] and re-emits whenever this entity's data
     * changes. Replaces the per-DAO `getDaily<X>Count(): Flow<Int>` Room queries.
     */
    fun countAfterFlow(afterTimestamp: Long): Flow<Int> = callbackFlow {
        fun current(): Int = countAfter(afterTimestamp).toInt()
        trySend(current())
        val subscription = boxStore.subscribe(entityClass).observer { _ -> trySend(current()) }
        awaitClose { subscription.cancel() }
    }.flowOn(Dispatchers.IO).conflate()
}
