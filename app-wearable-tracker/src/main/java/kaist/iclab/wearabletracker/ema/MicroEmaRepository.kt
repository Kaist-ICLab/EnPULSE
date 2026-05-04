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
     * 1. Sort questions by ID (ascending).
     * 2. Find the first question with an ID > last shown question ID.
     * 3. If none (end of list), wrap around to the first question (smallest ID).
     * 4. Update the last shown question ID in persistent storage.
     */
    fun getNextQuestion(config: WatchSurveyConfig): WatchQuestion? {
        if (config.questions.isEmpty()) return null

        // 1. Sort questions by ID
        val sortedQuestions = config.questions.sortedBy { it.id }

        // 2. Get the last shown question ID for this survey
        val lastId = prefsHelper.getLastQuestionId(config.surveyId)

        // 3. Find the next question in sequence
        val nextQuestion = sortedQuestions.find { it.id > lastId }
            ?: sortedQuestions.first() // Wrap around if we reached the end or it's the first time

        // 4. Update persistent storage with the new last shown ID
        prefsHelper.saveLastQuestionId(config.surveyId, nextQuestion.id)

        Log.d(TAG, "Queue update for Survey ${config.surveyId}: lastId=$lastId -> nextId=${nextQuestion.id}")
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
}
