package kaist.iclab.mobiletracker.webapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Developer test receiver to trigger WebApp notifications via ADB.
 *
 * Trigger command:
 * adb shell am broadcast -a kaist.iclab.mobiletracker.test.TRIGGER_WEBAPP \
 *   -n kaist.iclab.trackerSystem/kaist.iclab.mobiletracker.webapp.WebAppTestReceiver \
 *   --es webapp_id <webapp_id> \
 *   --es survey_id <survey_id>
 */
class WebAppTestReceiver : BroadcastReceiver(), KoinComponent {
    private val webAppTriggerHandler by inject<WebAppTriggerHandler>()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_TRIGGER_WEBAPP) {
            val webAppId = intent.getStringExtra(EXTRA_WEBAPP_ID) ?: "demo-webapp"
            val surveyId = intent.getStringExtra(EXTRA_SURVEY_ID) ?: "test_survey"

            Log.d(TAG, "Simulating WebApp trigger: webAppId=$webAppId, surveyId=$surveyId")
            try {
                webAppTriggerHandler.launch(surveyId, webAppId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to launch simulated WebApp trigger", e)
            }
        }
    }

    companion object {
        private const val TAG = "WebAppTestReceiver"
        const val ACTION_TRIGGER_WEBAPP = "kaist.iclab.mobiletracker.test.TRIGGER_WEBAPP"
        const val EXTRA_WEBAPP_ID = "webapp_id"
        const val EXTRA_SURVEY_ID = "survey_id"
    }
}
