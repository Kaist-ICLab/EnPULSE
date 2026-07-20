package kaist.iclab.wearabletracker.ema

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kaist.iclab.tracker.sensor.microema.MicroEmaResponse
import kaist.iclab.tracker.sensor.microema.ResponseStatus
import kaist.iclab.tracker.sensor.microema.WatchQuestion
import kaist.iclab.tracker.sensor.microema.WatchSurveyConfig
import kaist.iclab.wearabletracker.Constants
import kaist.iclab.wearabletracker.data.PhoneCommunicationManager
import kaist.iclab.wearabletracker.db.entity.MicroEmaResponseEntity
import kaist.iclab.wearabletracker.db.obx.MicroEmaResponseStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class MicroEmaResponseManager(
    private val context: Context,
    private val repository: MicroEmaRepository,
    private val store: MicroEmaResponseStore,
    private val phoneCommunicationManager: PhoneCommunicationManager,
    private val coroutineScope: CoroutineScope
) {
    companion object {
        private const val TAG = "MicroEmaRespMgr"
    }

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private var isListening = false

    fun startListening() {
        if (isListening) return
        isListening = true

        val bleChannel = phoneCommunicationManager.getBleChannel()

        bleChannel.addOnReceivedListener(setOf(Constants.BLE.KEY_MICRO_EMA_ACK)) { _, jsonElement ->
            val ackData = when (jsonElement) {
                is JsonPrimitive -> jsonElement.content
                else -> jsonElement.toString().trim('"')
            }
            handleAck(ackData)
        }

        bleChannel.addOnReceivedListener(setOf(Constants.BLE.KEY_MICRO_EMA_TRIGGER)) { _, jsonElement ->
            try {
                Log.d(TAG, "Received JIT trigger payload: $jsonElement")

                val triggerObj = when (jsonElement) {
                    is kotlinx.serialization.json.JsonObject -> jsonElement
                    is kotlinx.serialization.json.JsonPrimitive ->
                        json.parseToJsonElement(jsonElement.content).jsonObject
                    else -> json.parseToJsonElement(jsonElement.toString()).jsonObject
                }

                val surveyId = triggerObj["surveyId"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                val title = triggerObj["title"]?.jsonPrimitive?.content ?: "Micro EMA"
                val expireAfterMs = triggerObj["expireAfterMs"]?.jsonPrimitive?.content?.toLongOrNull()
                val questionJson = triggerObj["question"] ?: run {
                    Log.e(TAG, "Trigger payload missing 'question' field")
                    return@addOnReceivedListener
                }

                val question = json.decodeFromJsonElement<WatchQuestion>(questionJson)
                repository.updateConfig(
                    WatchSurveyConfig(
                        surveyId = surveyId,
                        title = title,
                        expireAfterMs = expireAfterMs,
                        questions = listOf(question)
                    )
                )
                launchMicroEma()
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing JIT trigger payload: ${e.message}", e)
            }
        }
    }

    private fun launchMicroEma() {
        try {
            // Trigger a double-buzz vibration to physically alert the user
            val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200), -1))

            val intent = Intent(context, WatchSurveyActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch WatchSurveyActivity: ${e.message}")
        }
    }

    private fun handleAck(ackData: String) {
        coroutineScope.launch {
            try {
                val ids = ackData.split(",").mapNotNull { it.trim().toLongOrNull() }
                if (ids.isNotEmpty()) {
                    store.markSynced(ids)
                    Log.d(TAG, "Marked ${ids.size} responses as synced via ACK")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing MicroEMA ACK: ${e.message}")
            }
        }
    }

    suspend fun saveAndSync(responses: List<MicroEmaResponse>) {
        val entities = responses.map { it.toEntity() }
        val ids = store.insertAll(entities)
        Log.d(TAG, "Saved ${entities.size} responses locally")

        try {
            syncResponsesToPhone(responses, ids)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync to phone (will retry later): ${e.message}")
        }
    }

    private suspend fun syncResponsesToPhone(responses: List<MicroEmaResponse>, idsToMark: List<Long>) {
        val payload = buildSyncPayload(responses, idsToMark)
        phoneCommunicationManager.getBleChannel().send(
            Constants.BLE.KEY_MICRO_EMA_RESPONSE,
            payload,
            isUrgent = true
        )
        Log.d(TAG, "Sent ${responses.size} responses to phone via BLE (waiting for ACK)")
    }

    suspend fun retrySyncUnsynced() {
        val unsynced = store.getUnsyncedResponses()
        if (unsynced.isEmpty()) return

        val responses = unsynced.map { it.toResponse() }
        val ids = unsynced.map { it.id }
        try {
            syncResponsesToPhone(responses, ids)
        } catch (e: Exception) {
            Log.w(TAG, "Retry sync failed: ${e.message}")
        }
    }

    private fun buildSyncPayload(responses: List<MicroEmaResponse>, ids: List<Long>): String {
        val payloadItems = responses.mapIndexed { index, r ->
            buildMap<String, String?> {
                put("id", ids.getOrNull(index)?.toString())
                put("surveyId", r.surveyId.toString())
                put("questionId", r.questionId.toString())
                put("value", if (r.status == ResponseStatus.ANSWERED) r.answer else null)
                put("status", r.status.name)
                put("triggerTime", r.triggerTime.toString())
                put("surveyStartTime", r.surveyStartTime.toString())
                put("responseTime", r.responseTime?.toString())
            }
        }
        return json.encodeToString(payloadItems)
    }

    private fun MicroEmaResponse.toEntity() = MicroEmaResponseEntity(
        surveyId = surveyId,
        questionId = questionId,
        answer = answer,
        status = status.name,
        triggerTime = triggerTime,
        surveyStartTime = surveyStartTime,
        responseTime = responseTime,
        isSynced = false
    )

    private fun MicroEmaResponseEntity.toResponse() = MicroEmaResponse(
        surveyId = surveyId,
        questionId = questionId,
        answer = answer,
        status = ResponseStatus.valueOf(status),
        triggerTime = triggerTime,
        surveyStartTime = surveyStartTime,
        responseTime = responseTime
    )
}
