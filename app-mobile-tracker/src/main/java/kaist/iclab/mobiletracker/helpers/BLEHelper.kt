package kaist.iclab.mobiletracker.helpers

import android.content.Context
import android.util.Log
import kaist.iclab.mobiletracker.config.AppConfig
import kaist.iclab.mobiletracker.data.DeviceType
import kaist.iclab.mobiletracker.data.survey.SurveyQuestionResponseInsert
import kaist.iclab.mobiletracker.db.entity.common.LocationEntity
import kaist.iclab.mobiletracker.db.entity.watch.WatchAccelerometerEntity
import kaist.iclab.mobiletracker.db.entity.watch.WatchEDAEntity
import kaist.iclab.mobiletracker.db.entity.watch.WatchHeartRateEntity
import kaist.iclab.mobiletracker.db.entity.watch.WatchPPGEntity
import kaist.iclab.mobiletracker.db.entity.watch.WatchSkinTemperatureEntity
import kaist.iclab.mobiletracker.di.AppCoroutineScope
import kaist.iclab.mobiletracker.repository.MicroEmaRepository
import kaist.iclab.mobiletracker.repository.Result
import kaist.iclab.mobiletracker.repository.UserProfileRepository
import kaist.iclab.mobiletracker.repository.WatchSensorRepository
import kaist.iclab.mobiletracker.db.dao.phone.MicroEmaResponseDao
import kaist.iclab.mobiletracker.db.entity.phone.MicroEmaResponseEntity
import kaist.iclab.mobiletracker.services.SurveyService
import kaist.iclab.mobiletracker.services.SyncTimestampService
import kaist.iclab.mobiletracker.utils.SensorDataCsvParser
import kaist.iclab.tracker.sync.ble.BLEDataChannel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Helper class for managing BLE communication with wearable devices.
 * Handles receiving sensor data via BLE and storing it locally in Room database.
 */
class BLEHelper(
    private val context: Context,
    private val watchSensorRepository: WatchSensorRepository,
    private val timestampService: SyncTimestampService,
    private val microEmaRepository: MicroEmaRepository,
    private val microEmaResponseDao: MicroEmaResponseDao
) : KoinComponent {
    private lateinit var bleChannel: BLEDataChannel

    // Injected coroutine scope
    private val appScope by inject<AppCoroutineScope>()
    private val surveyService by inject<SurveyService>()
    private val userProfileRepository by inject<UserProfileRepository>()

    private val isoFormatter = DateTimeFormatter.ISO_INSTANT

    fun initialize() {
        bleChannel = BLEDataChannel(context)
        setupListeners()
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
                        isSynced = false
                    )
                }

                if (responses.isNotEmpty()) {
                    microEmaResponseDao.insertAll(responses)
                    Log.d(
                        AppConfig.LogTags.PHONE_BLE,
                        "[MICRO_EMA] Cached ${responses.size} responses locally on phone"
                    )
                    // Send ACK back to watch immediately so it can clean up its DB
                    sendMicroEmaAck(processedIds)

                    // Attempt to upload immediately for real-time responsiveness
                    // We launch this in the IO dispatcher so it doesn't block BLE reception
                    appScope.io.launch {
                        surveyService.uploadUnsyncedMicroEmaResponses(microEmaResponseDao)
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
     * This uses a Just-in-Time architecture: the phone picks a question from the master config
     * and sends the entire question data to the watch.
     */
    fun triggerMicroEmaOnWatch() {
        appScope.io.launch {
            try {
                val config = microEmaRepository.loadConfig()
                if (config == null) {
                    Log.e(
                        AppConfig.LogTags.PHONE_BLE,
                        "[MICRO_EMA] Failed to load config, cannot trigger"
                    )
                    return@launch
                }

                // Pick a random question to send
                val question = config.questions.randomOrNull()
                if (question == null) {
                    Log.e(AppConfig.LogTags.PHONE_BLE, "[MICRO_EMA] Config has no questions")
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
                    "[MICRO_EMA] Sent JIT trigger to watch: ${question.text}"
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
     * Parse batch ID from the CSV header.
     * Expected format: "BATCH:uuid-string\nSINCE:timestamp\n---DATA---\n..."
     */
    private fun parseBatchId(csvData: String): String? {
        val lines = csvData.lines()
        for (line in lines) {
            if (line.startsWith("BATCH:")) {
                return line.removePrefix("BATCH:").trim()
            }
        }
        return null
    }

    /**
     * Send ACK (acknowledgment) back to watch after successful data processing.
     * Format: "batchId:status:endTimestamp"
     */
    private suspend fun sendAck(batchId: String, success: Boolean, endTimestamp: Long?) {
        try {
            val status = if (success) "OK" else "FAIL"
            val ackData = if (endTimestamp != null) {
                "$batchId:$status:$endTimestamp"
            } else {
                "$batchId:$status"
            }
            bleChannel.send(AppConfig.BLEKeys.SYNC_ACK, ackData)
            Log.d(
                AppConfig.LogTags.PHONE_BLE,
                "Sent ACK for batch $batchId: $status (ts=$endTimestamp)"
            )
        } catch (e: Exception) {
            Log.e(
                AppConfig.LogTags.PHONE_BLE,
                "Failed to send ACK for batch $batchId: ${e.message}",
                e
            )
        }
    }

    /**
     * Parse CHUNK_END_TS from the CSV header.
     * Expected format: "BATCH:...\nSINCE:...\nCHUNK_END_TS:123456789\n---DATA---"
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
     * Parse CSV data and extract all sensor data types, then store locally in Room database.
     * Uses managed coroutine scope for proper lifecycle management.
     */
    private fun parseAndStoreWatchData(csvData: String) {
        appScope.io.launch {
            // Extract batch ID for ACK (if present in new format)
            val batchId = parseBatchId(csvData)

            try {

                // Parse all sensor types from CSV
                val locationDataList = SensorDataCsvParser.parseLocationCsv(csvData)
                val accelerometerDataList = SensorDataCsvParser.parseAccelerometerCsv(csvData)
                val edaDataList = SensorDataCsvParser.parseEDACsv(csvData)
                val heartRateDataList = SensorDataCsvParser.parseHeartRateCsv(csvData)
                val ppgDataList = SensorDataCsvParser.parsePPGCsv(csvData)
                val skinTemperatureDataList = SensorDataCsvParser.parseSkinTemperatureCsv(csvData)

                // Convert Supabase data classes to Room entities and store locally
                var totalStored = 0
                var hasAnyData = false

                if (locationDataList.isNotEmpty()) {
                    hasAnyData = true
                    val entities = locationDataList.map { data ->
                        // Parse timestamp from "YYYY-MM-DD HH:mm:ss" back to milliseconds
                        LocationEntity(
                            eventId = data.eventId,
                            uuid = "", // Will be set by repository
                            deviceType = DeviceType.WATCH.value,
                            received = Instant.parse(data.received).toEpochMilli(),
                            timestamp = Instant.parse(data.timestamp).toEpochMilli(),
                            latitude = data.latitude,
                            longitude = data.longitude,
                            altitude = data.altitude,
                            speed = data.speed,
                            accuracy = data.accuracy
                        )
                    }
                    when (val result = watchSensorRepository.insertLocationData(entities)) {
                        is Result.Success -> {
                            totalStored += entities.size
                            Log.d(
                                AppConfig.LogTags.PHONE_BLE,
                                "Stored ${entities.size} location entries locally"
                            )
                        }

                        is Result.Error -> {
                            Log.e(
                                AppConfig.LogTags.PHONE_BLE,
                                "Failed to store location data: ${result.message}",
                                result.exception
                            )
                        }
                    }
                }

                if (accelerometerDataList.isNotEmpty()) {
                    hasAnyData = true
                    val entities = accelerometerDataList.map { data ->
                        WatchAccelerometerEntity(
                            eventId = data.eventId,
                            received = Instant.parse(data.received).toEpochMilli(),
                            timestamp = Instant.parse(data.timestamp).toEpochMilli(),
                            x = data.x,
                            y = data.y,
                            z = data.z
                        )
                    }
                    when (val result = watchSensorRepository.insertAccelerometerData(entities)) {
                        is Result.Success -> {
                            totalStored += entities.size
                            Log.d(
                                AppConfig.LogTags.PHONE_BLE,
                                "Stored ${entities.size} accelerometer entries locally"
                            )
                        }

                        is Result.Error -> {
                            Log.e(
                                AppConfig.LogTags.PHONE_BLE,
                                "Failed to store accelerometer data: ${result.message}",
                                result.exception
                            )
                        }
                    }
                }

                if (edaDataList.isNotEmpty()) {
                    hasAnyData = true
                    val entities = edaDataList.map { data ->
                        WatchEDAEntity(
                            eventId = data.eventId,
                            received = Instant.parse(data.received).toEpochMilli(),
                            timestamp = Instant.parse(data.timestamp).toEpochMilli(),
                            skinConductance = data.skinConductance,
                            status = data.status
                        )
                    }
                    when (val result = watchSensorRepository.insertEDAData(entities)) {
                        is Result.Success -> {
                            totalStored += entities.size
                            Log.d(
                                AppConfig.LogTags.PHONE_BLE,
                                "Stored ${entities.size} EDA entries locally"
                            )
                        }

                        is Result.Error -> {
                            Log.e(
                                AppConfig.LogTags.PHONE_BLE,
                                "Failed to store EDA data: ${result.message}",
                                result.exception
                            )
                        }
                    }
                }

                if (heartRateDataList.isNotEmpty()) {
                    hasAnyData = true
                    val entities = heartRateDataList.map { data ->
                        WatchHeartRateEntity(
                            eventId = data.eventId,
                            received = Instant.parse(data.received).toEpochMilli(),
                            timestamp = Instant.parse(data.timestamp).toEpochMilli(),
                            hr = data.hr,
                            hrStatus = data.hrStatus,
                            ibi = data.ibi,
                            ibiStatus = data.ibiStatus
                        )
                    }
                    when (val result = watchSensorRepository.insertHeartRateData(entities)) {
                        is Result.Success -> {
                            totalStored += entities.size
                            Log.d(
                                AppConfig.LogTags.PHONE_BLE,
                                "Stored ${entities.size} HeartRate entries locally"
                            )
                        }

                        is Result.Error -> {
                            Log.e(
                                AppConfig.LogTags.PHONE_BLE,
                                "Failed to store HeartRate data: ${result.message}",
                                result.exception
                            )
                        }
                    }
                }

                if (ppgDataList.isNotEmpty()) {
                    hasAnyData = true
                    val entities = ppgDataList.map { data ->
                        WatchPPGEntity(
                            eventId = data.eventId,
                            received = Instant.parse(data.received).toEpochMilli(),
                            timestamp = Instant.parse(data.timestamp).toEpochMilli(),
                            green = data.green,
                            greenStatus = data.greenStatus,
                            red = data.red,
                            redStatus = data.redStatus,
                            ir = data.ir,
                            irStatus = data.irStatus
                        )
                    }
                    when (val result = watchSensorRepository.insertPPGData(entities)) {
                        is Result.Success -> {
                            totalStored += entities.size
                            Log.d(
                                AppConfig.LogTags.PHONE_BLE,
                                "Stored ${entities.size} PPG entries locally"
                            )
                        }

                        is Result.Error -> {
                            Log.e(
                                AppConfig.LogTags.PHONE_BLE,
                                "Failed to store PPG data: ${result.message}",
                                result.exception
                            )
                        }
                    }
                }

                if (skinTemperatureDataList.isNotEmpty()) {
                    hasAnyData = true
                    val entities = skinTemperatureDataList.map { data ->
                        WatchSkinTemperatureEntity(
                            eventId = data.eventId,
                            received = Instant.parse(data.received).toEpochMilli(),
                            timestamp = Instant.parse(data.timestamp).toEpochMilli(),
                            ambientTemp = data.ambientTemp,
                            objectTemp = data.objectTemp,
                            status = data.status
                        )
                    }
                    when (val result = watchSensorRepository.insertSkinTemperatureData(entities)) {
                        is Result.Success -> {
                            totalStored += entities.size
                            Log.d(
                                AppConfig.LogTags.PHONE_BLE,
                                "Stored ${entities.size} SkinTemperature entries locally"
                            )
                        }

                        is Result.Error -> {
                            Log.e(
                                AppConfig.LogTags.PHONE_BLE,
                                "Failed to store SkinTemperature data: ${result.message}",
                                result.exception
                            )
                        }
                    }
                }

                if (hasAnyData && totalStored > 0) {
                    // Track when watch data is received
                    timestampService.updateLastWatchDataReceived()
                    Log.d(
                        AppConfig.LogTags.PHONE_BLE,
                        "Total stored: $totalStored sensor data entries locally"
                    )

                    // Send success ACK back to watch if batch ID is present
                    if (batchId != null) {
                        val chunkEndTs = parseChunkEndTimestamp(csvData)
                        sendAck(batchId, success = true, endTimestamp = chunkEndTs)
                    }
                } else {
                    Log.w(AppConfig.LogTags.PHONE_BLE, "No sensor data found in CSV")
                    // Still send ACK for empty but valid batch
                    if (batchId != null) {
                        val chunkEndTs = parseChunkEndTimestamp(csvData)
                        sendAck(batchId, success = true, endTimestamp = chunkEndTs)
                    }
                }
            } catch (e: Exception) {
                Log.e(
                    AppConfig.LogTags.PHONE_BLE,
                    "Error parsing or storing sensor data: ${e.message}",
                    e
                )
                // Send failure ACK if we have a batch ID
                if (batchId != null) {
                    val chunkEndTs = parseChunkEndTimestamp(csvData)
                    sendAck(batchId, success = false, endTimestamp = chunkEndTs)
                }
            }
        }
    }
}
