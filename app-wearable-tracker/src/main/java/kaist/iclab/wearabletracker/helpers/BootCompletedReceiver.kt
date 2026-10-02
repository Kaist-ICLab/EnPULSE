package kaist.iclab.wearabletracker.helpers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Starts the app's process after a reboot. Collection is deliberately not resumed: starting the
 * process runs WearableApplication, which marks a collection that was running before the reboot
 * as stopped and notifies the wearer to start it again from the app.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        Log.d(TAG, "Watch rebooted; collection stays stopped until it is started from the app.")
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
