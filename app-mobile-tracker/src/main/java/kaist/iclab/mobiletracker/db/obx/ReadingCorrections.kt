package kaist.iclab.mobiletracker.db.obx

import kaist.iclab.mobiletracker.db.entity.BaseEntity

/**
 * Decides which re-read readings need storing, for sensors that re-read the same time range on
 * every poll. There is one reading per (timestamp, deviceType), and the newest in [incoming] wins.
 *
 * - No stored reading yet: store it as new.
 * - Same [content] as the stored reading: drop it, so each poll doesn't upload everything again.
 * - Different [content]: a correction. It takes over the stored reading's eventId with no local
 *   id of its own, so ObjectBox's REPLACE gives it a new id past the upload cursor and it uploads
 *   again. Keeping the eventId means the server updates that record instead of adding another.
 */
internal fun <T : BaseEntity> selectReadingsToStore(
    incoming: List<T>,
    existing: List<T>,
    content: (T) -> Any?
): List<T> {
    val existingByKey = existing.associateBy { it.timestamp to it.deviceType }
    return incoming.associateBy { it.timestamp to it.deviceType }.mapNotNull { (key, reading) ->
        val stored = existingByKey[key] ?: return@mapNotNull reading
        if (content(stored) == content(reading)) return@mapNotNull null
        reading.apply {
            id = 0
            eventId = stored.eventId
        }
    }
}
