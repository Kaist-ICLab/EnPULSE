package kaist.iclab.tracker.listener.core

import android.view.accessibility.AccessibilityEvent

sealed class AccessibilityEventInfo {
    data class Event(val event: AccessibilityEvent?) : AccessibilityEventInfo()
    data object Interrupt : AccessibilityEventInfo()
}