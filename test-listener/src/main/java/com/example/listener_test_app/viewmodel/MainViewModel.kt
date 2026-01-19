package com.example.listener_test_app.viewmodel

import android.content.Intent
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kaist.iclab.tracker.listener.AccessibilityListener
import kaist.iclab.tracker.listener.AlarmListener
import kaist.iclab.tracker.listener.BroadcastListener
import kaist.iclab.tracker.listener.NotificationListener
import kaist.iclab.tracker.listener.core.AccessibilityEventInfo
import kaist.iclab.tracker.listener.core.NotificationEventInfo

class MainViewModel : ViewModel() {
    companion object {
        const val TAG = "ListenerTest"
    }

    private val notificationListener = NotificationListener()
    private val accessibilityListener = AccessibilityListener()

    private val broadcastAlarmCallbackList = mutableListOf<(Intent?) -> Unit>()
    private val notificationCallbackList = mutableListOf<(NotificationEventInfo) -> Unit>()
    private val accessibilityCallbackList = mutableListOf<(AccessibilityEventInfo) -> Unit>()

    var count by mutableIntStateOf(0)
        private set

    private fun getNewBroadcastAlarmCallback(
        broadcastActionName: String,
        alarmActionName: String,
    ): (Intent?) -> Unit {
        val currentCount = count
        return { intent: Intent? ->
            if (listOf(broadcastActionName, alarmActionName).contains(intent?.action)) {
                if (intent?.action == broadcastActionName)
                    Log.v(TAG, "Callback $currentCount: Broadcast received!")
                else if (intent?.action == alarmActionName)
                    Log.v(TAG, "Callback $currentCount: Alarm received!")
            }
        }
    }

    private fun getNewNotificationCallback(): (NotificationEventInfo) -> Unit {
        val currentCount = count
        return { event: NotificationEventInfo ->
            when (event) {
                is NotificationEventInfo.Posted -> Log.v(
                    TAG,
                    "Callback $currentCount: Notification posted"
                )

                else -> Log.v(TAG, "Callback $currentCount: Notification removed")
            }
            Unit
        }
    }

    private fun getNewAccessibilityCallback(): (AccessibilityEventInfo) -> Unit {
        val currentCount = count
        return { event: AccessibilityEventInfo ->
            when (event) {
                is AccessibilityEventInfo.Event -> Log.v(
                    TAG,
                    "Callback $currentCount: Event type ${event.event?.eventType} occurred"
                )

                else -> Log.v(TAG, "Callback $currentCount: Interrupt detected")
            }
            Unit
        }
    }

    fun addListener(
        broadcastActionName: String,
        alarmActionName: String,
        broadcastListener: BroadcastListener,
        alarmListener: AlarmListener,
    ) {
        val broadcastAlarmCallback =
            getNewBroadcastAlarmCallback(broadcastActionName, alarmActionName)
        val notificationCallback = getNewNotificationCallback()
        val accessibilityCallback = getNewAccessibilityCallback()

        broadcastAlarmCallbackList.add(broadcastAlarmCallback)
        notificationCallbackList.add(notificationCallback)
        accessibilityCallbackList.add(accessibilityCallback)

        broadcastListener.addListener(broadcastAlarmCallback)
        alarmListener.addListener(broadcastAlarmCallback)
        notificationListener.addListener(notificationCallback)
        accessibilityListener.addListener(accessibilityCallback)

        Log.v("test", "add $count")
        count += 1
    }

    fun removeListener(
        broadcastListener: BroadcastListener,
        alarmListener: AlarmListener,
    ) {
        if (count == 0) return

        val broadcastAlarmCallback = broadcastAlarmCallbackList.last()
        val notificationCallback = notificationCallbackList.last()
        val accessibilityCallback = accessibilityCallbackList.last()

        broadcastAlarmCallbackList.remove(broadcastAlarmCallback)
        notificationCallbackList.remove(notificationCallback)
        accessibilityCallbackList.remove(accessibilityCallback)

        broadcastListener.removeListener(broadcastAlarmCallback)
        alarmListener.removeListener(broadcastAlarmCallback)
        notificationListener.removeListener(notificationCallback)
        accessibilityListener.removeListener(accessibilityCallback)

        count--
    }
}