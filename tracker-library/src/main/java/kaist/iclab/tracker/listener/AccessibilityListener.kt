package kaist.iclab.tracker.listener

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import kaist.iclab.tracker.listener.core.AccessibilityEventInfo
import kaist.iclab.tracker.listener.core.Listener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AccessibilityListener: Listener<AccessibilityEventInfo> {
    class AccessibilityServiceAdaptor: AccessibilityService() {
        companion object {
            val receivers = mutableListOf<(AccessibilityEventInfo) -> Unit>()
        }

        fun addListener(listener: (AccessibilityEventInfo) -> Unit) {
            assert(!receivers.contains(listener))
            receivers.add(listener)
        }

        fun removeListener(listener: (AccessibilityEventInfo) -> Unit) {
            assert(receivers.contains(listener))
            receivers.remove(listener)
        }

        override fun onAccessibilityEvent(event: AccessibilityEvent?) {
//            Log.d(javaClass.simpleName, "onAccessibilityEvent: $event")

            // Use coroutine to prevent listeners from blocking each other
            for(callback in receivers) {
                CoroutineScope(Dispatchers.IO).launch {
                    callback(AccessibilityEventInfo.Event(event))
                }
            }
        }

        override fun onInterrupt() {
            for(callback in receivers) {
                CoroutineScope(Dispatchers.IO).launch {
                    callback(AccessibilityEventInfo.Interrupt)
                }
            }
        }
    }

    companion object {
        const val TAG = "AccessibilityTrigger"
    }

    override fun init() {}

    override fun addListener(listener: (AccessibilityEventInfo) -> Unit) {
        AccessibilityServiceAdaptor.receivers.add(listener)
    }

    override fun removeListener(listener: (AccessibilityEventInfo) -> Unit): Boolean {
        return AccessibilityServiceAdaptor.receivers.remove(listener)
    }
}