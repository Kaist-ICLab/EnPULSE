package kaist.iclab.wearabletracker.ema

import android.util.Log
import kaist.iclab.tracker.sensor.microema.WatchQuestion
import kaist.iclab.tracker.sensor.microema.WatchSurveyConfig
import kaist.iclab.wearabletracker.helpers.MicroEmaPreferencesHelper

/**
 * Repository that holds the active microEMA survey configuration and manages the question queue.
 *
 * Just-in-Time (JIT) Architecture:
 * This repository is populated dynamically when a BLE trigger is received from the phone.
 * It ensures questions are shown in a queue based on their ID.
 */
class MicroEmaRepository(
    private val prefsHelper: MicroEmaPreferencesHelper
) {
    companion object {
        private const val TAG = "MicroEmaRepo"
    }

    // In-memory holder for the active session configuration
    private var activeConfig: WatchSurveyConfig? = null

    @Volatile
    private var surveyActiveUntilMs = 0L

    /**
     * True while a survey is on screen; new triggers are dropped instead of replacing it.
     * Time-bounded rather than a plain flag: if a session never ends cleanly (e.g. the wearer
     * walks away from an open keyboard screen), it lapses instead of blocking every later trigger.
     */
    val isSurveyActive: Boolean
        get() = System.currentTimeMillis() < surveyActiveUntilMs

    /** Mark a survey as on screen for at most [forMs] from now; call again to extend. */
    fun markSurveyActive(forMs: Long) {
        surveyActiveUntilMs = System.currentTimeMillis() + forMs
    }

    fun markSurveyInactive() {
        surveyActiveUntilMs = 0L
    }

    /**
     * Get the active survey config received from the phone.
     */
    fun loadSurveyConfig(): WatchSurveyConfig? {
        Log.d(TAG, "Loading active config. Is null? ${activeConfig == null}")
        return activeConfig
    }

    /**
     * Get the next question in the queue for a given configuration.
     * Logic:
     * 1. Keep the config's question order: the phone sorts questions by the dashboard's
     *    position, so this follows the order the researcher set (sorting by id did not).
     * 2. Take the question after the last shown one.
     * 3. If none (end of list, first time, or the last one was removed), start from the first.
     * 4. Update the last shown question ID in persistent storage.
     */
    fun getNextQuestion(config: WatchSurveyConfig): WatchQuestion? {
        if (config.questions.isEmpty()) return null

        // 1. Questions in the order the phone sent them
        val orderedQuestions = config.questions

        // 2. Get the last shown question ID for this survey
        val lastId = prefsHelper.getLastQuestionId(config.surveyId)

        // 3. Find the next question in sequence
        val lastIndex = orderedQuestions.indexOfFirst { it.id == lastId }
        val nextQuestion = orderedQuestions.getOrNull(lastIndex + 1)
            ?: orderedQuestions.first() // Wrap around if we reached the end or it's the first time

        // 4. Update persistent storage with the new last shown ID
        prefsHelper.saveLastQuestionId(config.surveyId, nextQuestion.id)

        Log.d(
            TAG,
            "Queue update for Survey ${config.surveyId}: lastId=$lastId -> nextId=${nextQuestion.id}"
        )
        return nextQuestion
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

    /**
     * Reset the queue progress for a specific survey so it starts from the first question.
     */
    fun resetQueueProgress(surveyId: Int) {
        prefsHelper.clearLastQuestionId(surveyId)
    }
}
