package kaist.iclab.wearabletracker.data

import android.content.Context
import kaist.iclab.tracker.sensor.controller.ControllerState
import kaist.iclab.wearabletracker.ema.MicroEmaResponseManager
import kaist.iclab.wearabletracker.helpers.SyncPreferencesHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Manages automatic synchronization based on phone proximity and user-defined intervals.
 * Only syncs when data collection is actively running.
 */
class AutoSyncManager(
    private val context: Context,
    private val phoneCommunicationManager: PhoneCommunicationManager,
    private val microEmaResponseManager: MicroEmaResponseManager,
    private val syncPreferencesHelper: SyncPreferencesHelper,
    private val controllerStateFlow: StateFlow<ControllerState>,
    private val coroutineScope: CoroutineScope
) {
    companion object;

    private var syncJob: Job? = null

    fun evalSync() {
        if (syncJob?.isActive == true) return // Still processing previous evaluation

        syncJob = coroutineScope.launch(Dispatchers.IO) {
            val enabled = syncPreferencesHelper.isAutoSyncEnabled()
            val interval = syncPreferencesHelper.getAutoSyncInterval()
            val lastSync = syncPreferencesHelper.getLastSyncTimestamp()

            if (!enabled || interval <= 0L) {
                return@launch
            }

            // Only sync if data collection is actively running
            val controllerState = controllerStateFlow.value
            if (controllerState.flag != ControllerState.FLAG.RUNNING) {
                return@launch
            }

            val now = System.currentTimeMillis()
            val lastSyncTime = lastSync ?: 0L
            val elapsedTime = now - lastSyncTime

            if (elapsedTime >= interval) {
                // Check if device is in deep sleep (Doze mode)
                val powerManager =
                    context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                if (powerManager.isDeviceIdleMode) {
                    return@launch
                }

                // Check if phone is nearby before attempting sync
                if (!phoneCommunicationManager.isPhoneAvailable()) {
                    return@launch
                }

                phoneCommunicationManager.sendDataToPhone(isSilent = true)

                // Also retry any pending MicroEMA responses
                microEmaResponseManager.retrySyncUnsynced()
            }
        }
    }
}

