package kaist.iclab.mobiletracker.services.upload

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class PerKeyLockTest {

    @Test
    fun `work for the same key never overlaps`() = runBlocking {
        val lock = PerKeyLock()
        val running = AtomicInteger(0)
        val mostAtOnce = AtomicInteger(0)

        (1..20).map {
            async(Dispatchers.Default) {
                lock.withLock("Location") {
                    mostAtOnce.accumulateAndGet(running.incrementAndGet(), ::maxOf)
                    delay(2)
                    running.decrementAndGet()
                }
            }
        }.awaitAll()

        assertEquals(1, mostAtOnce.get())
    }

    @Test
    fun `different keys do not wait for each other`() = runBlocking {
        val lock = PerKeyLock()
        val otherKeyFinished = CompletableDeferred<Unit>()

        // Holds the first key until the second finishes, so a shared lock would time out here.
        withTimeout(5_000) {
            listOf(
                async(Dispatchers.Default) {
                    lock.withLock("Location") { otherKeyFinished.await() }
                },
                async(Dispatchers.Default) {
                    lock.withLock("Step") { otherKeyFinished.complete(Unit) }
                }
            ).awaitAll()
        }
        Unit
    }
}
