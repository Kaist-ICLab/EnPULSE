package kaist.iclab.wearabletracker.trigger

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kaist.iclab.tracker.trigger.state.DetectionStateTracker
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * A debug-only receiver that allows simulating sensor state changes via ADB.
 *
 * ## Usage (ADB)
 * ```bash
 * adb shell am broadcast -a kaist.iclab.wearabletracker.UPDATE_STATE --es sensor <NAME> --es value <VALUE>
 * ```
 *
 * Examples:
 * - Stress: `... --es sensor stress --es value High`
 * - Gesture: `... --es sensor gesture --es value Drinking`
 */
class TriggerDebugReceiver : BroadcastReceiver(), KoinComponent {

    private val tracker: DetectionStateTracker by inject()

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != ACTION_UPDATE_STATE) return

        val sensor = intent.getStringExtra(EXTRA_SENSOR)
        val value = intent.getStringExtra(EXTRA_VALUE)

        if (sensor != null && value != null) {
            Log.d(TAG, "Received debug state update via ADB: $sensor -> $value")
            tracker.updateState(sensor, value, System.currentTimeMillis())
        } else {
            Log.w(TAG, "Received UPDATE_STATE broadcast but missing 'sensor' or 'value' extras")
        }
    }

    companion object {
        private const val TAG = "TriggerDebugReceiver"
        const val ACTION_UPDATE_STATE = "kaist.iclab.wearabletracker.UPDATE_STATE"
        const val EXTRA_SENSOR = "sensor"
        const val EXTRA_VALUE = "value"
    }
}
