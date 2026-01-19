package kaist.iclab.tracker.listener

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import kaist.iclab.tracker.listener.core.Listener

class BroadcastListener(
    private val context: Context,
    private val actionNames: Array<String>,
) : Listener<Intent?> {
    // Stores receiver objects to Map, so they can be managed with listeners instead of receivers
    private val receivers = mutableMapOf<Int, BroadcastReceiver>()

    companion object {
        const val TAG = "BroadcastTrigger"
    }

    override fun init() {}

    override fun addListener(listener: (Intent?) -> Unit) {
        val hash = listener.hashCode()
        assert(!receivers.contains(hash))

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Log.d(TAG, "receiver:onReceive")
                intent?.apply { listener(this) }
            }
        }
        receivers[hash] = receiver

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            /*
            * From Tiramisu, we need to specify the receiver exported or not
            * One of RECEIVER_EXPORTED or RECEIVER_NOT_EXPORTED should be specified when a receiver isn't being registered exclusively for system broadcasts
            * */
            context.registerReceiver(receiver, IntentFilter().apply {
                actionNames.forEach { addAction(it) }
            }, Context.RECEIVER_EXPORTED)

        } else {
            context.registerReceiver(receiver, IntentFilter().apply {
                actionNames.forEach { addAction(it) }
            })
        }

        Log.d(TAG, "Registering broadcast receiver")
    }

    override fun removeListener(listener: (Intent?) -> Unit): Boolean {
        val hash = listener.hashCode()
        if (!receivers.contains(hash)) return false

        Log.d(TAG, "Unregistering broadcast receiver")
        val receiver = receivers.remove(hash)
        context.unregisterReceiver(receiver)
        return true
    }
}