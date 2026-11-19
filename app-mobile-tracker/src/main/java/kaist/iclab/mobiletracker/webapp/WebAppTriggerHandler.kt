package kaist.iclab.mobiletracker.webapp

import android.content.Context
import android.util.Log
import kaist.iclab.tracker.sensor.survey.SurveySchedule
import kaist.iclab.tracker.storage.core.SurveyScheduleStorage

/**
 * Launches web apps and notifications for the phone's trigger engine. Issues a [SurveySchedule] the same way
 * [kaist.iclab.tracker.sensor.phone.SurveySensor.triggerSurveyNotification] does, then shows a
 * notification that opens [WebAppActivity].
 */
class WebAppTriggerHandler(
    private val context: Context,
    private val scheduleStorage: SurveyScheduleStorage,
    private val webAppRegistry: WebAppRegistry
) {
    fun launch(surveyId: String, webAppId: String) {
        val webApp = webAppRegistry.get(webAppId)
        if (webApp == null) {
            Log.e(TAG, "Unknown webAppId: $webAppId")
            return
        }

        // Same pattern as SurveySensor.triggerSurveyNotification: triggerTime is recorded at
        // addSchedule time and never overwritten later.
        val scheduleId = scheduleStorage.addSchedule(
            SurveySchedule(surveyId = surveyId, triggerTime = System.currentTimeMillis())
        )

        WebAppNotificationBuilder.showWebAppTriggerNotification(
            context = context,
            webAppId = webAppId,
            surveyId = surveyId,
            scheduleId = scheduleId,
            baseUrl = webApp.url
        )
        scheduleStorage.setActualTriggerTime(scheduleId, System.currentTimeMillis())
    }

    fun launchNotification(title: String, body: String, url: String?) {
        WebAppNotificationBuilder.showGenericTriggerNotification(
            context = context,
            title = title,
            body = body,
            url = url,
            webAppRegistry = webAppRegistry
        )
    }

    companion object {
        private const val TAG = "WebAppTriggerHandler"
    }
}
