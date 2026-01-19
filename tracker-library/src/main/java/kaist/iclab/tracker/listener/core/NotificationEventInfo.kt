package kaist.iclab.tracker.listener.core

import android.service.notification.NotificationListenerService.RankingMap
import android.service.notification.StatusBarNotification

sealed class NotificationEventInfo(
    open val sbn: StatusBarNotification?, open val rankingMap: RankingMap?
) {
    data class Posted(
        override val sbn: StatusBarNotification?,
        override val rankingMap: RankingMap?
    ) :
        NotificationEventInfo(sbn, rankingMap)

    data class Removed(
        override val sbn: StatusBarNotification?,
        override val rankingMap: RankingMap?,
        val reason: Int
    ) :
        NotificationEventInfo(sbn, rankingMap)
}