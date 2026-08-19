package kaist.iclab.mobiletracker.helpers


import kotlinx.coroutines.CancellationException
import android.content.Context
import android.content.Intent
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
import kaist.iclab.mobiletracker.webapp.WebAppTriggerHandler
import kaist.iclab.tracker.sensor.galaxywatch.MicroEmaSensor
import kaist.iclab.tracker.sensor.phone.SurveySensor
import kaist.iclab.tracker.storage.core.StateStorage
import kaist.iclab.tracker.sync.ble.BLEDataChannel
import kaist.iclab.tracker.trigger.state.DetectionStateTracker
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import androidx.core.net.toUri

/**
 * Helper class for managing BLE communication with wearable devices.
 * Handles receiving sensor data via BLE and storing it locally in Room database.
 */
class BLEHelper(
    private val context: Context,
    private val watchSensorRepository: WatchSensorRepository,
    private val timestampService: SyncTimestampService,
    private val microEmaConfigStorage: StateStorage<MicroEmaSensor.Config>,
    private val microEmaResponseStore: MicroEmaResponseStore,
    private val detectionStateTracker: DetectionStateTracker,
) : KoinComponent {
    private lateinit var bleChannel: BLEDataChannel

    private val appScope by inject<AppCoroutineScope>()
    private val surveyService by inject<SurveyService>()
    private val surveySensor by inject<SurveySensor>()
    private val userProfileRepository by inject<UserProfileRepository>()
    private val webAppTriggerHandler by inject<WebAppTriggerHandler>()
    private val webAppRegistry by inject<kaist.iclab.mobiletracker.webapp.WebAppRegistry>()

    private var isInitialized = false

    // One mutex per sensorId so chunks for the same sensor are processed (and ACKed) strictly
    // in the order they were received - see parseAndStoreWatchData/computeAckEndTimestamp.
    private val sensorProcessingMutexes = java.util.concurrent.ConcurrentHashMap<String, Mutex>()

    private val parserBySensorId: Map<String, (String) -> List<BaseEntity>> = mapOf(
        Constants.SensorId.LOCATION to { csv -> SensorDataCsvParser.parseLocationCsv(csv) },
        Constants.SensorId.ACCELEROMETER to { csv -> SensorDataCsvParser.parseAccelerometerCsv(csv) },
        Constants.SensorId.EDA to { csv -> SensorDataCsvParser.parseEDACsv(csv) },
        Constants.SensorId.HEART_RATE to { csv -> SensorDataCsvParser.parseHeartRateCsv(csv) },
        Constants.SensorId.PPG to { csv -> SensorDataCsvParser.parsePPGCsv(csv) },
        Constants.SensorId.SKIN_TEMPERATURE to { csv -> SensorDataCsvParser.parseSkinTemperatureCsv(csv) },
        Constants.SensorId.IMU to { csv -> SensorDataCsvParser.parseIMUCsv(csv) },
        Constants.SensorId.GESTURE to { csv -> SensorDataCsvParser.parseGestureCsv(csv) },
        Constants.SensorId.STRESS to { csv -> SensorDataCsvParser.parseStressCsv(csv) },
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

        // Listen for phone EMA triggers from watch
        bleChannel.addOnReceivedListener(setOf(AppConfig.BLEKeys.PHONE_EMA_TRIGGER)) { _, json ->
            val surveyId = when {
                json is kotlinx.serialization.json.JsonPrimitive -> json.content
                else -> json.toString()
            }
            Log.d(AppConfig.LogTags.PHONE_BLE, "Received PHONE_EMA_TRIGGER for surveyId=$surveyId")
            surveySensor.triggerSurveyNotification(surveyId)
        }

        // Legacy listeners: the trigger engine now lives entirely on the phone
        // (PhoneTriggerActionHandler executes Ema/WatchEma/Broadcast/Notification actions
        // in-process), so nothing forwards these BLE keys from the watch anymore. Left in place
        // as harmless backward-compat handling per the migration plan, not actively exercised.
        bleChannel.addOnReceivedListener(setOf(AppConfig.BLEKeys.WEBAPP_TRIGGER)) { _, json ->
            webAppTriggerHandler.handleWebAppTriggerPayload(json)
        }

        bleChannel.addOnReceivedListener(setOf(AppConfig.BLEKeys.NOTIFICATION_TRIGGER)) { _, json ->
            webAppTriggerHandler.handleNotificationTriggerPayload(json)
        }

        bleChannel.addOnReceivedListener(setOf(AppConfig.BLEKeys.BROADCAST_TRIGGER)) { _, json ->
            webAppTriggerHandler.handleBroadcastTriggerPayload(json)
        }

        // Listen for the watch relaying a tapped watch-notification's url back to the phone
        // (see PhoneTriggerActionHandler.handleWatchNotification / the watch's
        // WatchNotificationTapReceiver) — the watch has no browser, so it just asks the phone to
        // open the url on its behalf.
        bleChannel.addOnReceivedListener(setOf(AppConfig.BLEKeys.PHONE_OPEN_URL_TRIGGER)) { _, json ->
            val url = when (json) {
                is kotlinx.serialization.json.JsonPrimitive -> json.content
                else -> json.toString()
            }
            handleOpenUrlTrigger(url)
        }

        // Listen for sensor detection state updates forwarded live from the watch's
        // StressDetectionAdapter/GestureDetectionAdapter (via the watch's DetectionStateForwarder)
        // — the trigger engine now runs on the phone, so these feed its DetectionStateTracker
        // directly. The same key is also used by TriggerDebugReceiver-style ADB/dashboard test
        // tooling to simulate states in the opposite direction; a real incoming payload here is
        // just another producer of the same {sensor: value} shape.
        bleChannel.addOnReceivedListener(setOf(AppConfig.BLEKeys.DETECTION_STATE_UPDATE)) { _, json ->
            handleDetectionStateUpdate(json)
        }
    }

    private fun handleOpenUrlTrigger(url: String) {
        try {
            // If the url's host matches a registered WebApp's allowedOrigin, it's a trusted,
            // known destination - open it in WebAppActivity so the page gets EnPulseBridge
            // (native sensor/storage/device access), same as any other WebApp trigger. Otherwise
            // fall back to the bridge-less SimpleWebViewActivity, since the url is arbitrary
            // dashboard-authored input with no associated WebApp registration.
            val trustedWebApp = webAppRegistry.findByUrl(url)
            val intent = if (trustedWebApp != null) {
                // Match WebAppNotificationBuilder.createWebAppActivityIntent's flags/data convention
                // (documentLaunchMode="intoExisting" + per-webapp data URI) so this gets the same
                // distinct-task-card behavior as any other WebApp launch.
                Intent(context, kaist.iclab.mobiletracker.webapp.WebAppActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    data = "webapp://${trustedWebApp.id}".toUri()
                    putExtra(kaist.iclab.mobiletracker.webapp.WebAppActivity.EXTRA_URL, url)
                    putExtra(kaist.iclab.mobiletracker.webapp.WebAppActivity.EXTRA_WEBAPP_ID, trustedWebApp.id)
                }
            } else {
                Intent(context, kaist.iclab.mobiletracker.webapp.SimpleWebViewActivity::class.java).apply {
                    putExtra(kaist.iclab.mobiletracker.webapp.SimpleWebViewActivity.EXTRA_URL, url)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            }
            context.startActivity(intent)
            Log.d(
                AppConfig.LogTags.PHONE_BLE,
                "Opened url relayed from watch (in-app, trusted=${trustedWebApp != null}): $url"
            )
        } catch (e: Exception) {
            Log.e(AppConfig.LogTags.PHONE_BLE, "Failed to open url relayed from watch: ${e.message}", e)
        }
    }

    private fun handleDetectionStateUpdate(json: kotlinx.serialization.json.JsonElement) {
        try {
            val payload = when (json) {
                is kotlinx.serialization.json.JsonObject -> json
                is kotlinx.serialization.json.JsonPrimitive -> Json.parseToJsonElement(json.content).jsonObject
                else -> Json.parseToJsonElement(json.toString()).jsonObject
            }

            val now = System.currentTimeMillis()
            payload.forEach { (sensor, valueElement) ->
                val value = valueElement.jsonPrimitive.content
                detectionStateTracker.updateState(sensor, value, now)
            }
        } catch (e: Exception) {
            Log.e(AppConfig.LogTags.PHONE_BLE, "[TRIGGER] Error applying detection state update: ${e.message}", e)
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
                if (e is CancellationException) throw e
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
                if (e is CancellationException) throw e
                Log.e(AppConfig.LogTags.PHONE_BLE, "[MICRO_EMA] Failed to send ACK: ${e.message}")
            }
        }
    }

    /**
     * Send simulated detection state updates to the watch.
     * This allows the phone to trigger generic evaluation rules on the watch.
     */
    fun sendDetectionStateUpdates(states: Map<String, String>) {
        appScope.io.launch {
            try {
                // Serialize map to JSON
                val payload = buildJsonObject {
                    states.forEach { (key, value) ->
                        put(key, value)
                    }
                }.toString()

                bleChannel.send(AppConfig.BLEKeys.DETECTION_STATE_UPDATE, payload)
                Log.d(
                    AppConfig.LogTags.PHONE_BLE,
                    "[TRIGGER] Sent state updates to watch: $payload"
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(
                    AppConfig.LogTags.PHONE_BLE,
                    "[TRIGGER] Failed to send state updates: ${e.message}"
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
                if (e is CancellationException) throw e
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
            if (e is CancellationException) throw e
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
     *
     * Bootstrap case: if the phone has *no* watermark at all for this sensor yet (fresh install,
     * cleared app data, etc.), there's nothing to check contiguity against - trust the watch's
     * own resume cursor (chunkStartTs) as the starting baseline instead of implicitly demanding
     * a chunk starting at epoch 0. Without this, a phone-side reset that happens after the watch
     * has already advanced (and pruned up to) a non-zero watermark is unrecoverable: the watch
     * has nothing left below its cursor to (re)send, so the phone's gap back to "0" can never
     * close and every future chunk for that sensor ACKs 0 forever.
     */
    private fun computeAckEndTimestamp(sensorId: String, chunkStartTs: Long?, chunkEndTs: Long?): Long {
        val contiguousUpTo = timestampService.getBleContiguousTimestamp(sensorId)
        if (contiguousUpTo == null) {
            val bootstrapped = chunkEndTs ?: chunkStartTs ?: 0L
            timestampService.setBleContiguousTimestamp(sensorId, bootstrapped)
            return bootstrapped
        }
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
        val sensorId = parseSensorId(csvData)
        if (sensorId == null) {
            Log.w(AppConfig.LogTags.PHONE_BLE, "No SENSOR header found in chunk, dropping")
            return
        }

        appScope.io.launch {
            // Serialize processing per sensor. computeAckEndTimestamp's gap-detection assumes
            // chunks are stored - and ACKed - in the same order the watch sent them. Without
            // this, every incoming chunk was its own unawaited coroutine racing on the IO
            // dispatcher: a later chunk could finish (and get ACKed) before an earlier one still
            // mid-insert, which looks exactly like a lost/out-of-order chunk to the gap check.
            // That permanently pinned the ACK at the last-confirmed watermark (often 0) instead
            // of advancing, which is why watermark-advance-gated resend throttling alone doesn't
            // fix it - the phone never stops reporting a gap in the first place.
            val mutex = sensorProcessingMutexes.computeIfAbsent(sensorId) { Mutex() }
            mutex.withLock {
                processSensorChunk(sensorId, csvData)
            }
        }
    }

    private suspend fun processSensorChunk(sensorId: String, csvData: String) {
        val chunkStartTs = parseChunkStartTimestamp(csvData)
        val chunkEndTs = parseChunkEndTimestamp(csvData)

        try {
            val parser = parserBySensorId[sensorId]
            if (parser == null) {
                Log.e(AppConfig.LogTags.PHONE_BLE, "Unknown sensorId in chunk: $sensorId")
                sendAck(sensorId, success = false, endTimestamp = null)
                return
            }

            val entities = parser(csvData)

            if (entities.isEmpty()) {
                Log.w(AppConfig.LogTags.PHONE_BLE, "No rows parsed for $sensorId chunk")
                // Still ACK - an empty but valid chunk still needs its range confirmed.
                sendAck(sensorId, success = true, endTimestamp = computeAckEndTimestamp(sensorId, chunkStartTs, chunkEndTs))
                return
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
            if (e is CancellationException) throw e
            Log.e(
                AppConfig.LogTags.PHONE_BLE,
                "Error parsing or storing $sensorId data: ${e.message}",
                e
            )
            sendAck(sensorId, success = false, endTimestamp = null)
        }
    }
}
