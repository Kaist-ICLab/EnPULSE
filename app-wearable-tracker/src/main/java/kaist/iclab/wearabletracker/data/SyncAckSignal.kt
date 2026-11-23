package kaist.iclab.wearabletracker.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds

/**
 * Lets [PhoneCommunicationManager]'s send loop wait for [SyncAckListener] to process an ACK.
 *
 * Race-free usage: read [version], then check the state you care about (watermark in prefs,
 * [failedSince]), and only then [awaitChange] with the version you read. An ACK handled between
 * the read and the wait bumps the version, so the wait returns at once instead of missing it.
 */
class SyncAckSignal {
    private val _version = MutableStateFlow(0L)
    private val lastFailAt = ConcurrentHashMap<String, Long>()

    val version: Long get() = _version.value

    /** Called by [SyncAckListener] after it has fully handled an ACK for [sensorId]. */
    fun onAckHandled(sensorId: String, ok: Boolean, now: Long = System.currentTimeMillis()) {
        if (!ok) lastFailAt[sensorId] = now
        _version.update { it + 1 }
    }

    fun failedSince(sensorId: String, since: Long): Boolean =
        (lastFailAt[sensorId] ?: Long.MIN_VALUE) >= since

    /** Suspends until an ACK is handled after [seenVersion], or [timeoutMs] passes. */
    suspend fun awaitChange(seenVersion: Long, timeoutMs: Long): Boolean =
        withTimeoutOrNull(timeoutMs.milliseconds) { _version.first { it > seenVersion }; true } ?: false
}
