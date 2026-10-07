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

    /**
     * SharedPreferences configuration for persisting trigger cooldowns.
     */
    object Throttling {
        const val PREFS_NAME = "kaist.iclab.tracker.trigger_throttling"
    }

    /**
     * Condition evaluation configuration.
     */
    object Evaluation {
        /**
         * How long a detection state stays eligible to satisfy a condition.
         *
         * [DetectionStateTracker][kaist.iclab.tracker.trigger.state.DetectionStateTracker]
         * never forgets a state, so without this an attendee's one-off gesture (e.g.
         * "Clapping") stays true forever and keeps re-firing the trigger every time any
         * other sensor updates, including for the next attendee who did nothing. Stress
         * refreshes well inside this window, so it is unaffected.
         */
        const val MAX_DETECTION_AGE_MILLIS = 60 * 1000L

        /**
         * Sensors whose state stays eligible longer than [MAX_DETECTION_AGE_MILLIS].
         *
         * Activity recognition reports changes, not a steady stream: once the phone stays
         * still it stops sending updates, so "Still" went stale after 60 s and a condition
         * like "Clapping AND Still" stopped matching while the phone was still lying there.
         * A new activity replaces the old one as soon as the phone moves.
         */
        val SENSOR_MAX_AGE_MILLIS: Map<String, Long> = mapOf(
            "physical_activity" to 30 * 60 * 1000L
        )
    }
}
