package kaist.iclab.wearabletracker.ema


import android.util.Log
import kaist.iclab.tracker.sensor.microema.WatchSurveyConfig

/**
 * Repository that holds the active microEMA survey configuration.
 *
 * Just-in-Time (JIT) Architecture: 
 * This repository no longer loads from local assets. Instead, it is populated
 * dynamically when a BLE trigger is received from the phone.
 */
class MicroEmaRepository {
    companion object {
        private const val TAG = "MicroEmaRepo"
    }

    // In-memory holder for the active session configuration
    private var activeConfig: WatchSurveyConfig? = null

    /**
     * Get the active survey config received from the phone.
     */
    fun loadSurveyConfig(): WatchSurveyConfig? {
        Log.d(TAG, "Loading active config. Is null? ${activeConfig == null}")
        return activeConfig
    }

    /**
     * Update the active config. Called when a new BLE trigger is received.
     */
    fun updateConfig(config: WatchSurveyConfig) {
        this.activeConfig = config
        Log.d(
            TAG,
            "Active config updated for Survey ID: ${config.surveyId} with ${config.questions.size} questions"
        )
    }

    /**
     * Clear the cached config.
     */
    fun clearCache() {
        activeConfig = null
    }
}
