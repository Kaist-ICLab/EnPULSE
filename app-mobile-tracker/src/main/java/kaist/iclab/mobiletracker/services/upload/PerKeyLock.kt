package kaist.iclab.mobiletracker.services.upload

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/** Runs work for the same key one at a time, while different keys still run concurrently. */
internal class PerKeyLock {
    private val locks = ConcurrentHashMap<String, Mutex>()

    suspend fun <T> withLock(key: String, action: suspend () -> T): T =
        locks.computeIfAbsent(key) { Mutex() }.withLock { action() }
}
