package kaist.iclab.tracker.listener

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kaist.iclab.tracker.listener.core.Listener
import kaist.iclab.tracker.listener.core.NotificationEventInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationListener : Listener<NotificationEventInfo> {
    class NotificationListenerServiceAdaptor : NotificationListenerService() {
        companion object {
            val receivers = mutableListOf<(NotificationEventInfo) -> Unit>()
        }

        fun addListener(listener: (NotificationEventInfo) -> Unit) {
            assert(!receivers.contains(listener))
            receivers.add(listener)
        }

        fun removeListener(listener: (NotificationEventInfo) -> Unit) {
            assert(receivers.contains(listener))
            receivers.remove(listener)
        }

        override fun onNotificationPosted(sbn: StatusBarNotification?, rankingMap: RankingMap?) {
            Log.v(TAG, "receiver: onNotificationPosted")
            super.onNotificationPosted(sbn, rankingMap)

            // Use coroutine to prevent listeners from blocking each other
            for (callback in receivers) {
                CoroutineScope(Dispatchers.IO).launch {
                    callback(
                        NotificationEventInfo.Posted(
                            sbn = sbn,
                            rankingMap = rankingMap
                        )
                    )
                }
            }
        }

        override fun onNotificationRemoved(
            sbn: StatusBarNotification?,
            rankingMap: RankingMap?,
            reason: Int
        ) {
            super.onNotificationRemoved(sbn, rankingMap, reason)
            Log.v(TAG, "receiver: onNotificationRemoved")

            // Use coroutine to prevent listeners from blocking each other
            for (callback in receivers) {
                CoroutineScope(Dispatchers.IO).launch {
                    callback(
                        NotificationEventInfo.Removed(
                            sbn = sbn,
                            rankingMap = rankingMap,
                            reason = reason
                        )
                    )
                }
            }
        }
    }

    companion object {
        const val TAG = "NotificationTrigger"
    }

    override fun init() {}

    override fun addListener(listener: (NotificationEventInfo) -> Unit) {
        NotificationListenerServiceAdaptor.receivers.add(listener)
    }

    override fun removeListener(listener: (NotificationEventInfo) -> Unit): Boolean {
        return NotificationListenerServiceAdaptor.receivers.remove(listener)
    }
}

