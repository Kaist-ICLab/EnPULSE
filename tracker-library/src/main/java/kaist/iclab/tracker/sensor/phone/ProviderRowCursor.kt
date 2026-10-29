package kaist.iclab.tracker.sensor.phone

import android.content.Context

/**
 * Persisted "last row seen" marker for polling a content provider (call log, SMS, MMS) by `_ID`.
 *
 * `_ID` only grows, so `_ID > last` returns each row exactly once, including rows written after
 * a poll with an earlier DATE (a call's DATE is its start, but the row is written when it ends).
 * A DATE window either re-reads rows on every poll or misses those late ones.
 */
internal class ProviderRowCursor(context: Context, private val key: String) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** The highest `_ID` already emitted, or null before the first poll. */
    fun lastId(): Long? = if (prefs.contains(key)) prefs.getLong(key, 0L) else null

    fun advanceTo(id: Long) {
        if (id > (lastId() ?: Long.MIN_VALUE)) prefs.edit().putLong(key, id).apply()
    }

    private companion object {
        const val PREFS_NAME = "provider_row_cursors"
    }
}
