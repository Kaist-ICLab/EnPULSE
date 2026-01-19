package kaist.iclab.tracker.sensor.survey.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import kaist.iclab.tracker.sensor.survey.activity.ui.SurveyScreen
import kaist.iclab.tracker.sensor.survey.activity.ui.theme.DefaultSurveyTheme

class DefaultSurveyActivity : SurveyActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            DefaultSurveyTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SurveyScreen(
                        survey = survey,
                        pushSurveyResult = { it -> pushSurveyResult(it); finish() },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}