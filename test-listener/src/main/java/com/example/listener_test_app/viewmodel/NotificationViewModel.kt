package com.example.listener_test_app.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import kaist.iclab.tracker.listener.NotificationListener
import kaist.iclab.tracker.listener.core.NotificationEventInfo

class NotificationViewModel : ViewModel() {
    private val notificationListener by mutableStateOf(NotificationListener())

    private val tag = "NotificationListenerApp"
    private val list = mutableListOf<(NotificationEventInfo) -> Unit>()

    fun addCallback() {
        val count = list.size
        val callback = { event: NotificationEventInfo ->
            when (event) {
                is NotificationEventInfo.Posted -> Log.v(
                    tag,
                    "Callback $count: Notification posted"
                )

                else -> Log.v(tag, "Callback $count: Notification removed")
            }
            Unit
        }

        list.add(callback)
        notificationListener.addListener(callback)
    }

    fun removeCallback() {
        if (list.size > 0) {
            val lastCallback = list.last()
            list.remove(lastCallback)
            notificationListener.removeListener(lastCallback)
        }
    }
}