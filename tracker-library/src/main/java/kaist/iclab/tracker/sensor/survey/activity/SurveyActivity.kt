package kaist.iclab.tracker.sensor.survey.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.core.app.NotificationManagerCompat
import kaist.iclab.tracker.sensor.survey.Survey
import kaist.iclab.tracker.sensor.phone.SurveySensor
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

abstract class SurveyActivity : ComponentActivity() {
    companion object {
        lateinit var survey: Survey

        /** Returns the requested survey, or null if it is no longer in the current config. */
        lateinit var initSurvey: (String, String?) -> Survey?

        /** Intent extra: id of the notification that opened this survey; cancelled on open. */
        const val EXTRA_NOTIFICATION_ID = "notificationId"

        private val TAG = SurveyActivity::class.simpleName

        private fun canInitSurvey() = this::initSurvey.isInitialized
    }

    /**
     * False when the requested survey could not be loaded and the activity is finishing.
     * Subclasses must not render the survey then.
     */
    protected var isSurveyReady = false
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // The notification is ongoing and opened via a full-screen intent, so tapping never
        // auto-cancels it; left in the tray it would reopen this survey for the next person.
        if (intent.hasExtra(EXTRA_NOTIFICATION_ID)) {
            NotificationManagerCompat.from(this).cancel(intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0))
        }

        // The survey can be gone, e.g. a leftover notification tapped after a campaign switch.
        val surveyId = intent.getStringExtra("id")
        val scheduleId = intent.getStringExtra("scheduleId")
        val requested = if (surveyId != null && canInitSurvey()) initSurvey(surveyId, scheduleId) else null
        if (requested == null) {
            Log.w(TAG, "Survey $surveyId is not available; closing")
            finish()
            return
        }
        survey = requested
        isSurveyReady = true
    }

    fun pushSurveyResult(result: JsonElement) {
        val scheduleId = intent.getStringExtra("scheduleId")
        val stringResult = Json.encodeToString(result)
        Log.d(TAG, "survey result: $stringResult")
        val intent = Intent(SurveySensor.RESULT_ACTION_NAME).apply {
            putExtra("result", stringResult)
            putExtra("responseTime", System.currentTimeMillis())
            putExtra("scheduleId", scheduleId)
        }

        Log.d(TAG, "Send broadcast: $intent")
        sendBroadcast(intent)
    }
}
