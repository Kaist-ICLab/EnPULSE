package kaist.iclab.mobiletracker.helpers

import android.util.Log
import kaist.iclab.mobiletracker.di.AppCoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Manages the conditions and timing for triggering MicroEMA surveys on the watch.
 */
class MicroEmaTriggerManager(
    private val bleHelper: BLEHelper
) : KoinComponent {
    companion object {
        private const val TAG = "MicroEmaTriggerMgr"
    }

    private val appScope by inject<AppCoroutineScope>()

    private var testTriggerJob: Job? = null

    private val _isTestModeEnabled = MutableStateFlow(false)
    val isTestModeEnabled = _isTestModeEnabled.asStateFlow()

    /**
     * Start a periodic timer to trigger surveys for testing.
     * @param intervalMinutes Frequency of the trigger.
     */
    fun startTestTimer(intervalMinutes: Int = 1) {
        if (testTriggerJob?.isActive == true) return

        _isTestModeEnabled.value = true
        testTriggerJob = appScope.io.launch {
            Log.d(TAG, "Test Mode Started: Triggering every $intervalMinutes minute(s)")
            while (_isTestModeEnabled.value) {
                // Wait for the interval (convert minutes to ms)
                delay(intervalMinutes * 60 * 1000L)

                if (_isTestModeEnabled.value) {
                    Log.d(TAG, "Timer fired! Requesting MicroEMA trigger via BLE...")
                    bleHelper.triggerMicroEmaOnWatch()
                }
            }
        }
    }

    /**
     * Stop the periodic testing timer.
     */
    fun stopTestTimer() {
        _isTestModeEnabled.value = false
        testTriggerJob?.cancel()
        testTriggerJob = null
        Log.d(TAG, "Test Mode Stopped")
    }
}
