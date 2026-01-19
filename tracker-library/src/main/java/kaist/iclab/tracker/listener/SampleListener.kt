package kaist.iclab.tracker.listener

import kaist.iclab.tracker.listener.core.Listener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SampleListener(
    val duration: Long
) : Listener<Long> {
    private val listeners = mutableListOf<(Long) -> Unit>()
    private var job: Job? = null
    override fun init() {}

    override fun addListener(listener: (Long) -> Unit) {
        listeners.add(listener)
        if (listeners.size == 1) {
            assert(job == null)
            job = CoroutineScope(Dispatchers.IO).launch {
                while (isActive) {
                    listeners.forEach { it(System.currentTimeMillis()) }
                    delay(duration)
                }
            }
        }
    }

    override fun removeListener(listener: (Long) -> Unit): Boolean {
        if (listeners.isEmpty()) {
            job?.cancel()
            job = null
        }

        return true
    }
}