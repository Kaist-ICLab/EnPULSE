package com.example.listener_test_app.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import kaist.iclab.tracker.listener.AccessibilityListener
import kaist.iclab.tracker.listener.core.AccessibilityEventInfo

class AccessibilityViewModel : ViewModel() {
    private val accessibilityListener by mutableStateOf(AccessibilityListener())

    private val tag = "AccessibilityListenerApp"
    private val list = mutableListOf<(AccessibilityEventInfo) -> Unit>()

    fun addCallback() {
        val count = list.size
        val callback = { event: AccessibilityEventInfo ->
            when (event) {
                is AccessibilityEventInfo.Event -> Log.v(
                    tag,
                    "Callback $count: Event type ${event.event?.eventType} occurred"
                )

                else -> Log.v(tag, "Callback $count: Interrupt detected")
            }
            Unit
        }

        list.add(callback)
        accessibilityListener.addListener(callback)
    }

    fun removeCallback() {
        if (list.size > 0) {
            val lastCallback = list.last()
            list.remove(lastCallback)
            accessibilityListener.removeListener(lastCallback)
        }
    }
}