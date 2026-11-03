package kaist.iclab.tracker.listener

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import kaist.iclab.tracker.listener.core.Listener


class AlarmListener(
    private val context: Context,
    private val actionName: String,
    private val actionCode: Int,
    private val actionIntervalInMilliseconds: Long,
) : Listener<Intent?> {
    // Stores receiver objects to Map, so they can be managed with listeners instead of receivers
    private val receivers = mutableMapOf<Int, BroadcastReceiver>()

    override fun init() {}

    private val pendingIntent by lazy {
        PendingIntent.getBroadcast(
            context,
            actionCode,
            Intent(actionName).setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private val alarmManager by lazy {
        context.getSystemService<AlarmManager>()!!
    }

    override fun addListener(listener: (Intent?) -> Unit) {
        val hash = listener.hashCode()
        assert(!receivers.contains(hash))

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                listener(intent)
            }
        }
        receivers[hash] = receiver
        // Not exported: only this app's own alarm PendingIntent should be able to trigger it.
        ContextCompat.registerReceiver(
            context, receiver, IntentFilter(actionName), ContextCompat.RECEIVER_NOT_EXPORTED
        )

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis(),
            actionIntervalInMilliseconds,
            pendingIntent
        )

    }

    override fun removeListener(listener: (Intent?) -> Unit): Boolean {
        val hash = listener.hashCode()

        if (!receivers.contains(hash)) return false

        val receiver = receivers.remove(hash)
        context.unregisterReceiver(receiver)

        if (receivers.isEmpty()) alarmManager.cancel(pendingIntent)
        return true
    }
}