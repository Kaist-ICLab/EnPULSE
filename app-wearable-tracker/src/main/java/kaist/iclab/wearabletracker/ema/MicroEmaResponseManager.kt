package kaist.iclab.wearabletracker.ema

import android.util.Log
import kaist.iclab.wearabletracker.Constants
import kaist.iclab.wearabletracker.data.PhoneCommunicationManager
import kaist.iclab.wearabletracker.db.dao.MicroEmaResponseDao
import kaist.iclab.wearabletracker.db.entity.MicroEmaResponseEntity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Manages microEMA response persistence (Room) and sync (BLE → phone → Supabase).
 *
 * Flow:
 * 1. User answers questions on watch
 * 2. Responses saved to local Room DB [`micro_ema_responses`]
 * 3. Responses sent to phone via BLE channel
 * 4. Phone uploads to Supabase using existing [SurveyService.submitSurveyResponses]
 */
class MicroEmaResponseManager(
    private val dao: MicroEmaResponseDao,
    private val phoneCommunicationManager: PhoneCommunicationManager,
    private val coroutineScope: CoroutineScope
) {
    companion object {
        private const val TAG = "MicroEmaRespMgr"
    }

    private val json = Json { prettyPrint = false }

    /**
     * Start listening for ACKs from the phone.
     */
    fun startListening() {
        phoneCommunicationManager.getBleChannel().addOnReceivedListener(
            setOf(Constants.BLE.KEY_MICRO_EMA_ACK)
        ) { _, jsonElement ->
            val ackData = when (jsonElement) {
                is kotlinx.serialization.json.JsonPrimitive -> jsonElement.content
                else -> jsonElement.toString().trim('"')
            }
            handleAck(ackData)
        }
    }

    private fun handleAck(ackData: String) {
        coroutineScope.launch {
            try {
                val ids = ackData.split(",").mapNotNull { it.trim().toLongOrNull() }
                if (ids.isNotEmpty()) {
                    dao.markSynced(ids)
                    Log.d(TAG, "Marked ${ids.size} responses as synced via ACK")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing MicroEMA ACK: ${e.message}")
            }
        }
    }

    /**
     * Persist a list of responses locally and attempt to sync to phone.
     */
    suspend fun saveAndSync(responses: List<MicroEmaResponse>) {
        val entities = responses.map { it.toEntity() }

        // 1. Persist in Room
        val ids = dao.insertAll(entities)
        Log.d(TAG, "Saved ${entities.size} responses locally")

        // 2. Attempt sync to phone
        try {
            syncResponsesToPhone(responses, ids)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync to phone (will retry later): ${e.message}")
        }
    }

    /**
     * Send responses to the phone via BLE channel.
     * The phone parses this JSON and uploads to Supabase.
     */
    private suspend fun syncResponsesToPhone(responses: List<MicroEmaResponse>, idsToMark: List<Long>) {
        val payload = buildSyncPayload(responses, idsToMark)
        phoneCommunicationManager.getBleChannel().send(
            Constants.BLE.KEY_MICRO_EMA_RESPONSE,
            payload,
            isUrgent = true
        )
        Log.d(TAG, "Sent ${responses.size} responses to phone via BLE (waiting for ACK)")
        
        // DO NOT mark as synced here anymore. 
        // We wait for KEY_MICRO_EMA_ACK.
    }

    /**
     * Retry syncing any previously failed responses.
     * Call this periodically or when phone connectivity is restored.
     */
    suspend fun retrySyncUnsynced() {
        val unsynced = dao.getUnsyncedResponses()
        if (unsynced.isEmpty()) return

        val responses = unsynced.map { it.toResponse() }
        val ids = unsynced.map { it.id }
        try {
            syncResponsesToPhone(responses, ids)
        } catch (e: Exception) {
            Log.w(TAG, "Retry sync failed: ${e.message}")
        }
    }

    /**
     * Build a JSON payload for BLE transmission.
     *
     * Format per the user's spec:
     *   ANSWERED  → {"questionId":101, "value":"3", "status":"ANSWERED", ...}
     *   EXPIRED   → {"questionId":101, "value":null, "status":"EXPIRED", ...}
     *   DISMISSED → {"questionId":101, "value":null, "status":"DISMISSED", ...}
     */
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
        synced = false
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
