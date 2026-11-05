package kaist.iclab.tracker.listener

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import kaist.iclab.tracker.listener.core.Listener

class BroadcastListener(
    private val context: Context,
    private val actionNames: Array<String>,
) : Listener<Intent?> {
    // Stores receiver objects to Map, so they can be managed with listeners instead of receivers
    private val receivers = mutableMapOf<Int, BroadcastReceiver>()

    override fun init() {}

    override fun addListener(listener: (Intent?) -> Unit) {
        val hash = listener.hashCode()
        assert(!receivers.contains(hash))

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                intent?.apply { listener(this) }
            }
        }
        receivers[hash] = receiver

        // Not exported: system broadcasts are still delivered, and the app's own broadcasts and
        // PendingIntents (survey results, activity recognition) come from this app.
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter().apply { actionNames.forEach { addAction(it) } },
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

    }

    override fun removeListener(listener: (Intent?) -> Unit): Boolean {
        val hash = listener.hashCode()
        if (!receivers.contains(hash)) return false

        val receiver = receivers.remove(hash)
        context.unregisterReceiver(receiver)
        return true
    }
}