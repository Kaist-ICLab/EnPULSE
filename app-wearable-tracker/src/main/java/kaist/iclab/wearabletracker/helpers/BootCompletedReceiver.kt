package kaist.iclab.wearabletracker.helpers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kaist.iclab.tracker.sensor.controller.BackgroundController
import kaist.iclab.tracker.sensor.controller.ControllerState
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Automatically restarts sensor collection if the watch is rebooted.
 *
 * This receiver listens for [Intent.ACTION_BOOT_COMPLETED]. It checks the persisted
 * controller state and resumes collection if it was active before the reboot.
 */
class BootCompletedReceiver : BroadcastReceiver(), KoinComponent {

    private val sensorController: BackgroundController by inject()

    @android.annotation.SuppressLint("MissingPermission")
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.d(TAG, "Watch rebooted. Checking if sensor collection should resume...")

        val currentState = sensorController.controllerStateFlow.value

        // The BackgroundController uses persistent storage for its state.
        // If it was RUNNING before the reboot, we should restart the service.
        if (currentState.flag == ControllerState.FLAG.RUNNING) {
            Log.i(TAG, "Resuming background sensor collection after reboot.")
            try {
                sensorController.start()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restart sensor collection: ${e.message}", e)
            }
        } else {
            Log.d(TAG, "Collection was not active (State: ${currentState.flag}). No action taken.")
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
