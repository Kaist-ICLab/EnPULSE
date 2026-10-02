package kaist.iclab.wearabletracker.helpers

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kaist.iclab.tracker.sensor.controller.BackgroundController
import kaist.iclab.tracker.sensor.controller.BackgroundControllerDependenciesProvider
import kaist.iclab.tracker.sensor.controller.ControllerState
import kaist.iclab.tracker.sensor.core.SensorState
import kaist.iclab.tracker.storage.core.StateStorage
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

/**
 * Automatically restarts sensor collection if the watch is rebooted.
 *
 * This receiver listens for [Intent.ACTION_BOOT_COMPLETED]. It checks the persisted
 * controller state and resumes collection if it was active before the reboot.
 */
class BootCompletedReceiver : BroadcastReceiver(), KoinComponent {

    private val sensorController: BackgroundController by inject()
    private val controllerStateStorage: StateStorage<ControllerState> by inject(
        named("watchControllerStateStorage")
    )

    /** Whether the sensors collection would run include one that records audio (gesture). */
    private fun needsMicrophone(context: Context): Boolean {
        val provider = context.applicationContext as? BackgroundControllerDependenciesProvider
            ?: return false
        return provider.provideBackgroundControllerDependencies().sensors.any {
            it.sensorStateFlow.value.flag == SensorState.FLAG.ENABLED &&
                it.permissions.contains(Manifest.permission.RECORD_AUDIO)
        }
    }

    @android.annotation.SuppressLint("MissingPermission")
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.d(TAG, "Watch rebooted. Checking if sensor collection should resume...")

        val currentState = sensorController.controllerStateFlow.value

        // The BackgroundController uses persistent storage for its state.
        // If it was RUNNING or PAUSED before the reboot, we should restart the service.
        // PAUSED means the user intended collection to be active (just off-wrist at the time).
        if (currentState.flag == ControllerState.FLAG.RUNNING ||
            currentState.flag == ControllerState.FLAG.PAUSED
        ) {
            val ctx = context ?: return
            if (needsMicrophone(ctx)) {
                // Android refuses (14+) or silences (11-13) a microphone foreground service
                // started from BOOT_COMPLETED, so gesture collection can't resume from here.
                // Report it as stopped instead of claiming to run, and ask the wearer to reopen
                // the app, which restarts collection from the foreground.
                Log.i(TAG, "Collection needs the microphone; asking the user to resume it.")
                controllerStateStorage.set(ControllerState(ControllerState.FLAG.READY))
                NotificationHelper.showResumeCollectionNotification(ctx)
                return
            }
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
