package kaist.iclab.mobiletracker.viewmodels.settings

import androidx.lifecycle.ViewModel
import kaist.iclab.tracker.sensor.survey.SurveySensor
import kotlinx.coroutines.flow.map

class SurveySettingsViewModel(
    private val surveySensor: SurveySensor
) : ViewModel() {

    val config = surveySensor.configStateFlow

    val surveyList = config.map { configObj ->
        configObj.survey.keys.toList()
    }

    fun triggerSurvey(surveyId: String) {
        surveySensor.openSurvey(surveyId)
    }
}
