package kaist.iclab.mobiletracker.db.obx

import io.objectbox.BoxStore
import io.objectbox.query.QueryBuilder
import kaist.iclab.mobiletracker.db.entity.common.LocationEntity
import kaist.iclab.mobiletracker.db.entity.common.LocationEntity_

/**
 * Location is the one sensor stored for both phone and watch in a single box, partitioned by
 * `deviceType`. This store adds the deviceType-scoped operations the upload handlers and
 * repositories need, on top of the generic [SensorStore] used for the (device-agnostic) Data
 * screen and home-count reads.
 */
class LocationStore(boxStore: BoxStore) : SensorStore<LocationEntity>(
    boxStore,
    LocationEntity::class.java,
    LocationEntity_.id,
    LocationEntity_.timestamp,
    LocationEntity_.eventId
) {
    fun hasDataAfterByDeviceType(afterTimestamp: Long, deviceType: Int): Boolean =
        box.query()
            .equal(LocationEntity_.deviceType, deviceType.toLong())
            .greater(timestampProperty, afterTimestamp)
            .build().use { it.count() > 0 }

    fun countByDeviceType(deviceType: Int): Int =
        box.query()
            .equal(LocationEntity_.deviceType, deviceType.toLong())
            .build().use { it.count().toInt() }

    fun latestTimestampByDeviceType(deviceType: Int): Long? {
        val query = box.query()
            .equal(LocationEntity_.deviceType, deviceType.toLong())
            .build()
        return query.use {
            if (it.count() == 0L) null else it.property(timestampProperty).max()
        }
    }

    fun recordsAfterByDeviceType(
        afterTimestamp: Long,
        isAscending: Boolean,
        limit: Int,
        offset: Int,
        deviceType: Int
    ): List<LocationEntity> {
        val builder = box.query()
            .equal(LocationEntity_.deviceType, deviceType.toLong())
            .greaterOrEqual(timestampProperty, afterTimestamp)
        builder.order(timestampProperty, if (isAscending) 0 else QueryBuilder.DESCENDING)
        return builder.build().use { it.find(offset.toLong(), limit.toLong()) }
    }

    fun removeBeforeByDeviceType(timestamp: Long, deviceType: Int): Long =
        box.query()
            .equal(LocationEntity_.deviceType, deviceType.toLong())
            .less(timestampProperty, timestamp)
            .build().use { it.remove() }
}
