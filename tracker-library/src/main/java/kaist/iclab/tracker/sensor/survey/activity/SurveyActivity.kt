package kaist.iclab.tracker.sensor.survey.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import kaist.iclab.tracker.sensor.survey.Survey
import kaist.iclab.tracker.sensor.survey.SurveySensor
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

abstract class SurveyActivity : ComponentActivity() {
    companion object {
        lateinit var survey: Survey
        lateinit var initSurvey: (String, String?) -> Survey

        private val TAG = SurveyActivity::class.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val surveyId = intent.getStringExtra("id")!!
        val scheduleId = intent.getStringExtra("scheduleId")
        survey = initSurvey(surveyId, scheduleId)
    }

    fun pushSurveyResult(result: JsonElement) {
        val scheduleId = intent.getStringExtra("scheduleId")
        val stringResult = Json.encodeToString(result)
        Log.d(TAG, "survey result: $stringResult")
        Intent()
        val intent = Intent(SurveySensor.RESULT_ACTION_NAME).apply {
            putExtra("result", stringResult)
            putExtra("responseTime", System.currentTimeMillis())
            putExtra("scheduleId", scheduleId)
        }

        Log.d(TAG, "Send broadcast: $intent")
        sendBroadcast(intent)
    }
}