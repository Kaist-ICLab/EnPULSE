package kaist.iclab.wearabletracker.helpers

import android.content.Context
import android.content.SharedPreferences

/**
 * Helper utility for managing MicroEMA survey state using SharedPreferences.
 * Specifically used for implementing the question queue (tracking last shown question per survey).
 */
class MicroEmaPreferencesHelper(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    /**
     * Save the last shown question ID for a specific survey.
     */
    fun saveLastQuestionId(surveyId: Int, questionId: Int) {
        sharedPreferences.edit()
            .putInt(KEY_LAST_QUESTION_ID_PREFIX + surveyId, questionId)
            .apply()
    }

    /**
     * Get the last shown question ID for a specific survey.
     * Returns -1 if no question has been shown yet.
     */
    fun getLastQuestionId(surveyId: Int): Int {
        return sharedPreferences.getInt(KEY_LAST_QUESTION_ID_PREFIX + surveyId, -1)
    }

    /**
     * Clear the last shown question ID for a specific survey.
     */
    fun clearLastQuestionId(surveyId: Int) {
        sharedPreferences.edit()
            .remove(KEY_LAST_QUESTION_ID_PREFIX + surveyId)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "micro_ema_preferences"
        private const val KEY_LAST_QUESTION_ID_PREFIX = "last_q_id_"
    }
}
