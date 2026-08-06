package kaist.iclab.wearabletracker.trigger

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kaist.iclab.wearabletracker.Constants
import kaist.iclab.wearabletracker.data.PhoneCommunicationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Fired when the user taps a watch notification for a [kaist.iclab.tracker.trigger.model.TriggerActionConfig.Notification]
 * action that had a `url` — the watch has no browser of its own, so this just relays the url back
 * to the phone (`Constants.BLE.KEY_PHONE_OPEN_URL_TRIGGER`), where
 * [kaist.iclab.mobiletracker.helpers.BLEHelper] opens it. Registered via [PendingIntent.getBroadcast]
 * from [WatchNotificationTriggerReceiver], not an intent-filter — see AndroidManifest.xml.
 */
class WatchNotificationTapReceiver : BroadcastReceiver(), KoinComponent {

    companion object {
        private const val TAG = "WatchNotificationTap"
        const val EXTRA_URL = "url"
    }

    private val phoneCommunicationManager: PhoneCommunicationManager by inject()
    private val coroutineScope: CoroutineScope by inject()

    override fun onReceive(context: Context?, intent: Intent?) {
        val url = intent?.getStringExtra(EXTRA_URL)
        if (url == null) {
            Log.w(TAG, "Received tap with no url extra")
            return
        }

        // Relaying over BLE is a suspend call — extend the receiver's lifetime with goAsync()
        // until it completes, since onReceive itself must return quickly.
        val pendingResult = goAsync()
        coroutineScope.launch {
            try {
                phoneCommunicationManager.getBleChannel().send(Constants.BLE.KEY_PHONE_OPEN_URL_TRIGGER, url, isUrgent = true)
                Log.d(TAG, "Relayed url to phone: $url")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to relay url to phone: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
