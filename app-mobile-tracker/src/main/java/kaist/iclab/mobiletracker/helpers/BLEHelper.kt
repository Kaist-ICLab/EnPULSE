package kaist.iclab.mobiletracker.helpers


import android.content.Context
import android.util.Log
import kaist.iclab.mobiletracker.Constants
import kaist.iclab.mobiletracker.config.AppConfig
import kaist.iclab.mobiletracker.db.entity.BaseEntity
import kaist.iclab.mobiletracker.db.obx.MicroEmaResponseStore
import kaist.iclab.mobiletracker.db.entity.phone.MicroEmaResponseEntity
import kaist.iclab.mobiletracker.di.AppCoroutineScope
import kaist.iclab.mobiletracker.repository.Result
import kaist.iclab.mobiletracker.repository.UserProfileRepository
import kaist.iclab.mobiletracker.repository.WatchSensorRepository
import kaist.iclab.mobiletracker.services.SurveyService
import kaist.iclab.mobiletracker.services.SyncTimestampService
import kaist.iclab.mobiletracker.utils.SensorDataCsvParser
import kaist.iclab.tracker.sensor.galaxywatch.MicroEmaSensor
import kaist.iclab.tracker.storage.core.StateStorage
import kaist.iclab.tracker.sync.ble.BLEDataChannel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Helper class for managing BLE communication with wearable devices.
 * Handles receiving sensor data via BLE and storing it locally in Room database.
 */
class BLEHelper(
    private val context: Context,
    private val watchSensorRepository: WatchSensorRepository,
    private val timestampService: SyncTimestampService,
    private val microEmaConfigStorage: StateStorage<MicroEmaSensor.Config>,
    private val microEmaResponseStore: MicroEmaResponseStore
) : KoinComponent {
    private lateinit var bleChannel: BLEDataChannel

    // Injected coroutine scope
    private val appScope by inject<AppCoroutineScope>()
    private val surveyService by inject<SurveyService>()
    private val userProfileRepository by inject<UserProfileRepository>()

    private var isInitialized = false

    private val parserBySensorId: Map<String, (String) -> List<BaseEntity>> = mapOf(
        Constants.SensorId.LOCATION to { csv -> SensorDataCsvParser.parseLocationCsv(csv) },
        Constants.SensorId.ACCELEROMETER to { csv -> SensorDataCsvParser.parseAccelerometerCsv(csv) },
        Constants.SensorId.EDA to { csv -> SensorDataCsvParser.parseEDACsv(csv) },
        Constants.SensorId.HEART_RATE to { csv -> SensorDataCsvParser.parseHeartRateCsv(csv) },
        Constants.SensorId.PPG to { csv -> SensorDataCsvParser.parsePPGCsv(csv) },
        Constants.SensorId.SKIN_TEMPERATURE to { csv -> SensorDataCsvParser.parseSkinTemperatureCsv(csv) },
        Constants.SensorId.ECG to { csv -> SensorDataCsvParser.parseECGCsv(csv) },
    )

    fun initialize() {
        if (isInitialized) {
            Log.d(AppConfig.LogTags.PHONE_BLE, "BLEHelper already initialized, skipping...")
            return
        }
        bleChannel = BLEDataChannel(context)
        setupListeners()
        isInitialized = true
        Log.d(AppConfig.LogTags.PHONE_BLE, "BLEHelper initialized successfully")
    }

    /**
     * Cleanup method to cancel all coroutines when BLEHelper is no longer needed.
     * Should be called when the helper is being destroyed.
     */
    fun cleanup() {
        // No need to cancel scope as it's app-level
    }

    private fun setupListeners() {
        // Listen for sensor CSV data from watch
        bleChannel.addOnReceivedListener(setOf(AppConfig.BLEKeys.SENSOR_DATA_CSV)) { _, json ->
            val csvData = when {
                json is kotlinx.serialization.json.JsonPrimitive -> json.content
                else -> json.toString()
            }
            // Parse CSV and store all sensor data locally
            parseAndStoreWatchData(csvData)
        }

        // Listen for microEMA responses from watch
        bleChannel.addOnReceivedListener(setOf(AppConfig.BLEKeys.MICRO_EMA_RESPONSE)) { _, json ->
            val jsonString = when {
                json is kotlinx.serialization.json.JsonPrimitive -> json.content
                else -> json.toString()
            }
            handleMicroEmaResponse(jsonString)
        }
    }

    /**
     * Handle microEMA responses received from the watch via BLE.
     *
     * Expected payload format (JSON array):
     * [{"questionId":"101","value":"3","status":"ANSWERED","triggerTime":"17...","surveyStartTime":"17...","responseTime":"17..."}]
     *
     * For EXPIRED/DISMISSED, "value" is null.
     */
    private fun handleMicroEmaResponse(jsonString: String) {
        appScope.io.launch {
            try {
                val uuid = userProfileRepository.getCurrentUuid()
                if (uuid == null) {
                    Log.w(AppConfig.LogTags.PHONE_BLE, "[MICRO_EMA] No user UUID available")
                    return@launch
                }

                val jsonArray = Json.parseToJsonElement(jsonString).jsonArray
                val processedIds = mutableListOf<Long>()

                val responses = jsonArray.mapNotNull { element ->
                    val obj = element.jsonObject
                    val watchId = obj["id"]?.jsonPrimitive?.content?.toLongOrNull()
                    val questionId = obj["questionId"]?.jsonPrimitive?.content?.toIntOrNull()
                        ?: return@mapNotNull null
                    val value = obj["value"]?.let {
                        if (it is JsonNull) null else it.jsonPrimitive.content
                    }
                    val status = obj["status"]?.jsonPrimitive?.content ?: "ANSWERED"
                    val triggerTime = obj["triggerTime"]?.jsonPrimitive?.content
                    val surveyStartTime = obj["surveyStartTime"]?.jsonPrimitive?.content
                    val responseTime = obj["responseTime"]?.jsonPrimitive?.content

                    // Build the response JSON that goes into the `response` column
                    val responseJson = buildJsonObject {
                        put("id", questionId)
                        put("value", value)
                        put("status", status)
                    }

                    if (watchId != null) processedIds.add(watchId)

                    MicroEmaResponseEntity(
                        watchId = watchId ?: 0L,
                        uuid = uuid,
                        questionId = questionId,
                        triggerTime = triggerTime,
                        actualTriggerTime = triggerTime,
                        surveyStartTime = surveyStartTime,
                        responseSubmissionTime = responseTime,
                        responseJson = responseJson.toString(),
                        deviceType = 1,
                        isSynced = false
                    )
                }

                if (responses.isNotEmpty()) {
                    microEmaResponseStore.insertAll(responses)
                    Log.d(
                        AppConfig.LogTags.PHONE_BLE,
                        "[MICRO_EMA] Cached ${responses.size} responses locally on phone"
                    )
                    // Send ACK back to watch immediately so it can clean up its DB
                    sendMicroEmaAck(processedIds)

                    // Attempt to upload immediately for real-time responsiveness
                    // We launch this in the IO dispatcher so it doesn't block BLE reception
                    appScope.io.launch {
                        surveyService.uploadUnsyncedMicroEmaResponses(microEmaResponseStore)
                    }
                }
            } catch (e: Exception) {
                Log.e(
                    AppConfig.LogTags.PHONE_BLE,
                    "[MICRO_EMA] Error processing response: ${e.message}", e
                )
            }
        }
    }

    /**
     * Send ACK back to watch for successfully processed microEMA responses.
     * Format: List of watch database IDs as a comma-separated string.
     */
    private fun sendMicroEmaAck(ids: List<Long>) {
        if (ids.isEmpty()) return
        appScope.io.launch {
            try {
                val ackData = ids.joinToString(",")
                bleChannel.send(AppConfig.BLEKeys.MICRO_EMA_ACK, ackData)
                Log.d(AppConfig.LogTags.PHONE_BLE, "[MICRO_EMA] Sent ACK for IDs: $ackData")
            } catch (e: Exception) {
                Log.e(AppConfig.LogTags.PHONE_BLE, "[MICRO_EMA] Failed to send ACK: ${e.message}")
            }
        }
    }

    /**
     * Send a remote trigger to the watch to immediately start a microEMA survey session.
     * This uses a Just-in-Time architecture: the phone picks a survey from the retrieved configs
     * and sends a random question from it to the watch.
     */
    fun triggerMicroEmaOnWatch() {
        appScope.io.launch {
            try {
                // Get dynamic configs from storage
                val dynamicConfigs = microEmaConfigStorage.get().watchSurveyConfigs
                val config = dynamicConfigs.values.randomOrNull()

                if (config == null) {
                    Log.e(
                        AppConfig.LogTags.PHONE_BLE,
                        "[MICRO_EMA] No dynamic MicroEMA config available, cannot trigger"
                    )
                    return@launch
                }

                // Pick a random question to send
                val question = config.questions.randomOrNull()
                if (question == null) {
                    Log.e(
                        AppConfig.LogTags.PHONE_BLE,
                        "[MICRO_EMA] Config (ID: ${config.surveyId}) has no questions"
                    )
                    return@launch
                }

                // Wrap in a mini-config for the watch
                val triggerPayload = buildJsonObject {
                    put("surveyId", config.surveyId)
                    put("title", config.title)
                    put("expireAfterMs", config.expireAfterMs)
                    put("question", Json.encodeToJsonElement(question))
                }.toString()


                bleChannel.send(AppConfig.BLEKeys.MICRO_EMA_TRIGGER, triggerPayload)
                Log.d(
                    AppConfig.LogTags.PHONE_BLE,
                    "[MICRO_EMA] Sent JIT trigger to watch: ${question.text} (ID: ${config.surveyId})"
                )
            } catch (e: Exception) {
                Log.e(
                    AppConfig.LogTags.PHONE_BLE,
                    "[MICRO_EMA] Failed to send trigger: ${e.message}"
                )
            }
        }
    }

    /**
     * Push the campaign's active watch-sensor list to the watch, so it only collects/shows
     * sensors that are actually part of the wearer's campaign.
     */
    fun sendActiveSensorConfig(activeSensorIds: Collection<String>) {
        appScope.io.launch {
            try {
                val payload = buildJsonArray {
                    activeSensorIds.forEach { add(it) }
                }.toString()

                bleChannel.send(AppConfig.BLEKeys.ACTIVE_SENSOR_CONFIG, payload)
                Log.d(
                    AppConfig.LogTags.PHONE_BLE,
                    "Sent active sensor config to watch: $activeSensorIds"
                )
            } catch (e: Exception) {
                Log.e(
                    AppConfig.LogTags.PHONE_BLE,
                    "Failed to send active sensor config: ${e.message}"
                )
            }
        }
    }

    /**
     * Parse the sensor ID from the CSV header.
     * Expected format: "SENSOR:sensorId\nCHUNK_START_TS:...\nCHUNK_END_TS:...\n---DATA---\n..."
     */
    private fun parseSensorId(csvData: String): String? {
        val lines = csvData.lines()
        for (line in lines) {
            if (line.startsWith("SENSOR:")) {
                return line.removePrefix("SENSOR:").trim()
            }
        }
        return null
    }

    /**
     * Send ACK (acknowledgment) back to watch after processing a chunk.
     * Format: "sensorId:status:endTimestamp" - endTimestamp is the phone's verified-contiguous
     * watermark for this sensor, not necessarily this chunk's own raw end (see
     * parseAndStoreWatchData's gap-detection logic).
     */
    private suspend fun sendAck(sensorId: String, success: Boolean, endTimestamp: Long?) {
        try {
            val status = if (success) "OK" else "FAIL"
            val ackData = if (endTimestamp != null) {
                "$sensorId:$status:$endTimestamp"
            } else {
                "$sensorId:$status"
            }
            bleChannel.send(AppConfig.BLEKeys.SYNC_ACK, ackData)
            Log.d(
                AppConfig.LogTags.PHONE_BLE,
                "Sent ACK for $sensorId: $status (ts=$endTimestamp)"
            )
        } catch (e: Exception) {
            Log.e(
                AppConfig.LogTags.PHONE_BLE,
                "Failed to send ACK for $sensorId: ${e.message}",
                e
            )
        }
    }

    /**
     * Parse CHUNK_START_TS from the CSV header - the cursor the watch used to fetch this chunk.
     */
    private fun parseChunkStartTimestamp(csvData: String): Long? {
        val lines = csvData.lines()
        for (line in lines) {
            if (line.startsWith("CHUNK_START_TS:")) {
                return line.removePrefix("CHUNK_START_TS:").trim().toLongOrNull()
            }
        }
        return null
    }

    /**
     * Parse CHUNK_END_TS from the CSV header.
     * Expected format: "SENSOR:...\nCHUNK_START_TS:...\nCHUNK_END_TS:123456789\n---DATA---"
     */
    private fun parseChunkEndTimestamp(csvData: String): Long? {
        val lines = csvData.lines()
        for (line in lines) {
            if (line.startsWith("CHUNK_END_TS:")) {
                return line.removePrefix("CHUNK_END_TS:").trim().toLongOrNull()
            }
        }
        return null
    }

    /**
     * Compute the timestamp to ACK for this sensor: only advances past this chunk's own end if
     * the chunk is contiguous with what's already verified received (chunkStartTs <= the phone's
     * current watermark). If there's a gap - an earlier chunk was lost or arrived out of order -
     * re-affirms the old watermark instead of advancing past the hole, so the watch's cumulative
     * ACK matching never prunes data the phone doesn't actually have a contiguous copy of.
     */
    private fun computeAckEndTimestamp(sensorId: String, chunkStartTs: Long?, chunkEndTs: Long?): Long {
        val contiguousUpTo = timestampService.getBleContiguousTimestamp(sensorId) ?: 0L
        return if (chunkStartTs != null && chunkStartTs <= contiguousUpTo) {
            val advanced = maxOf(contiguousUpTo, chunkEndTs ?: contiguousUpTo)
            timestampService.setBleContiguousTimestamp(sensorId, advanced)
            advanced
        } else {
            contiguousUpTo
        }
    }

    /**
     * Parse a single-sensor CSV chunk and store it locally, then ACK.
     * Uses managed coroutine scope for proper lifecycle management.
     */
    private fun parseAndStoreWatchData(csvData: String) {
        appScope.io.launch {
            val sensorId = parseSensorId(csvData)
            if (sensorId == null) {
                Log.w(AppConfig.LogTags.PHONE_BLE, "No SENSOR header found in chunk, dropping")
                return@launch
            }

            val chunkStartTs = parseChunkStartTimestamp(csvData)
            val chunkEndTs = parseChunkEndTimestamp(csvData)

            try {
                val parser = parserBySensorId[sensorId]
                if (parser == null) {
                    Log.e(AppConfig.LogTags.PHONE_BLE, "Unknown sensorId in chunk: $sensorId")
                    sendAck(sensorId, success = false, endTimestamp = null)
                    return@launch
                }

                val entities = parser(csvData)

                if (entities.isEmpty()) {
                    Log.w(AppConfig.LogTags.PHONE_BLE, "No rows parsed for $sensorId chunk")
                    // Still ACK - an empty but valid chunk still needs its range confirmed.
                    sendAck(sensorId, success = true, endTimestamp = computeAckEndTimestamp(sensorId, chunkStartTs, chunkEndTs))
                    return@launch
                }

                when (val result = watchSensorRepository.insertBatch(sensorId, entities)) {
                    is Result.Success -> {
                        timestampService.updateLastWatchDataReceived()
                        Log.d(AppConfig.LogTags.PHONE_BLE, "Stored ${entities.size} $sensorId entries locally")
                        sendAck(sensorId, success = true, endTimestamp = computeAckEndTimestamp(sensorId, chunkStartTs, chunkEndTs))
                    }
                    is Result.Error -> {
                        Log.e(AppConfig.LogTags.PHONE_BLE, "Failed to store $sensorId data: ${result.message}", result.exception)
                        sendAck(sensorId, success = false, endTimestamp = null)
                    }
                }
            } catch (e: Exception) {
                Log.e(
                    AppConfig.LogTags.PHONE_BLE,
                    "Error parsing or storing $sensorId data: ${e.message}",
                    e
                )
                sendAck(sensorId, success = false, endTimestamp = null)
            }
        }
    }
}
