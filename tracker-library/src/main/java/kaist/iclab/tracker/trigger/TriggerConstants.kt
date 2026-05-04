package kaist.iclab.tracker.trigger

/**
 * Global constants for the Trigger Engine.
 */
object TriggerConstants {
    /**
     * WakeLock configuration to prevent CPU sleep during condition evaluation.
     */
    object WakeLock {
        const val TAG = "EnPulse:TriggerEngineWakeLock"
        
        /**
         * Maximum time in milliseconds to hold the WakeLock during evaluation.
         * Prevents infinite battery drain if a process hangs.
         */
        const val TIMEOUT_MS = 10 * 1000L
    }
}
