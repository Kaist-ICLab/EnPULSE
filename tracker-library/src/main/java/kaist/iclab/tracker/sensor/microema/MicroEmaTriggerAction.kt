package kaist.iclab.tracker.sensor.microema

import android.util.Log
import kaist.iclab.tracker.sync.ble.BLEDataChannel
import kaist.iclab.tracker.trigger.TriggerAction
import kaist.iclab.tracker.trigger.TriggerRule
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement

/**
 * TriggerAction that sends a MicroEMA trigger to the watch via BLE.
 */
class MicroEmaTriggerAction(
    private val bleChannel: BLEDataChannel,
    private val surveyConfigs: Map<Int, WatchSurveyConfig>
) : TriggerAction {

    companion object {
        private const val TAG = "MicroEmaTriggerAction"
    }

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun execute(rule: TriggerRule, calculatedValue: Double) {
        val surveyId = rule.action.payload?.toIntOrNull() ?: run {
            Log.e(TAG, "No surveyId found in rule action payload")
            return
        }

        val config = surveyConfigs[surveyId] ?: run {
            Log.e(TAG, "No WatchSurveyConfig found for surveyId: $surveyId")
            return
        }

        // We pick the first question for now as a "simple trigger"
        // In the future, this can be extended to send multiple or randomized questions
        val firstQuestion = config.questions.firstOrNull() ?: run {
            Log.e(TAG, "No questions found in config for surveyId: $surveyId")
            return
        }

        val triggerPayload = json.encodeToJsonElement(
            mapOf(
                "surveyId" to config.surveyId.toString(),
                "title" to config.title,
                "expireAfterMs" to config.expireAfterMs?.toString(),
                "question" to json.encodeToJsonElement(firstQuestion)
            )
        ).toString()

        Log.d(TAG, "Sending MicroEMA trigger for survey $surveyId to watch")
        bleChannel.send(
            EmaConstants.BLEKeys.MICRO_EMA_TRIGGER,
            triggerPayload,
            isUrgent = true
        )
    }
}
