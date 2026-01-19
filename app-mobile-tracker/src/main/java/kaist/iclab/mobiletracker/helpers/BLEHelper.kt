package kaist.iclab.mobiletracker.helpers

import java.time.Instant
import android.content.Context
import android.util.Log
import kaist.iclab.mobiletracker.config.AppConfig
import kaist.iclab.mobiletracker.data.DeviceType
import kaist.iclab.mobiletracker.db.entity.watch.WatchAccelerometerEntity
import kaist.iclab.mobiletracker.db.entity.watch.WatchEDAEntity
import kaist.iclab.mobiletracker.db.entity.watch.WatchHeartRateEntity
import kaist.iclab.mobiletracker.db.entity.common.LocationEntity
import kaist.iclab.mobiletracker.db.entity.watch.WatchPPGEntity
import kaist.iclab.mobiletracker.db.entity.watch.WatchSkinTemperatureEntity
import kaist.iclab.mobiletracker.repository.Result
import kaist.iclab.mobiletracker.repository.WatchSensorRepository
import kaist.iclab.mobiletracker.services.SyncTimestampService
import kaist.iclab.mobiletracker.utils.SensorDataCsvParser
import kaist.iclab.tracker.sync.ble.BLEDataChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Helper class for managing BLE communication with wearable devices.
 * Handles receiving sensor data via BLE and storing it locally in Room database.
 */
class BLEHelper(
    private val context: Context,
    private val watchSensorRepository: WatchSensorRepository,
    private val timestampService: SyncTimestampService
) {
    private lateinit var bleChannel: BLEDataChannel
    
    // Create a managed coroutine scope that can be cancelled
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun initialize() {
        bleChannel = BLEDataChannel(context)
        setupListeners()
    }
    
    /**
     * Cleanup method to cancel all coroutines when BLEHelper is no longer needed.
     * Should be called when the helper is being destroyed.
     */
    fun cleanup() {
        ioScope.cancel()
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
     */
    private suspend fun sendAck(batchId: String, success: Boolean) {
        try {
            val ackData = "$batchId:${if (success) "OK" else "FAIL"}"
            bleChannel.send(AppConfig.BLEKeys.SYNC_ACK, ackData)
            Log.d(AppConfig.LogTags.PHONE_BLE, "Sent ACK for batch $batchId: ${if (success) "OK" else "FAIL"}")
        } catch (e: Exception) {
            Log.e(AppConfig.LogTags.PHONE_BLE, "Failed to send ACK for batch $batchId: ${e.message}", e)
        }
    }

    /**
     * Parse CSV data and extract all sensor data types, then store locally in Room database.
     * Uses managed coroutine scope for proper lifecycle management.
     */
    private fun parseAndStoreWatchData(csvData: String) {
        ioScope.launch {
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
                            Log.d(AppConfig.LogTags.PHONE_BLE, "Stored ${entities.size} location entries locally")
                        }
                        is Result.Error -> {
                            Log.e(AppConfig.LogTags.PHONE_BLE, "Failed to store location data: ${result.message}", result.exception)
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
                            Log.d(AppConfig.LogTags.PHONE_BLE, "Stored ${entities.size} accelerometer entries locally")
                        }
                        is Result.Error -> {
                            Log.e(AppConfig.LogTags.PHONE_BLE, "Failed to store accelerometer data: ${result.message}", result.exception)
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
                            Log.d(AppConfig.LogTags.PHONE_BLE, "Stored ${entities.size} EDA entries locally")
                        }
                        is Result.Error -> {
                            Log.e(AppConfig.LogTags.PHONE_BLE, "Failed to store EDA data: ${result.message}", result.exception)
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
                            Log.d(AppConfig.LogTags.PHONE_BLE, "Stored ${entities.size} HeartRate entries locally")
                        }
                        is Result.Error -> {
                            Log.e(AppConfig.LogTags.PHONE_BLE, "Failed to store HeartRate data: ${result.message}", result.exception)
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
                            Log.d(AppConfig.LogTags.PHONE_BLE, "Stored ${entities.size} PPG entries locally")
                        }
                        is Result.Error -> {
                            Log.e(AppConfig.LogTags.PHONE_BLE, "Failed to store PPG data: ${result.message}", result.exception)
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
                            Log.d(AppConfig.LogTags.PHONE_BLE, "Stored ${entities.size} SkinTemperature entries locally")
                        }
                        is Result.Error -> {
                            Log.e(AppConfig.LogTags.PHONE_BLE, "Failed to store SkinTemperature data: ${result.message}", result.exception)
                        }
                    }
                }
                
                if (hasAnyData && totalStored > 0) {
                    // Track when watch data is received
                    timestampService.updateLastWatchDataReceived()
                    Log.d(AppConfig.LogTags.PHONE_BLE, "Total stored: $totalStored sensor data entries locally")
                    
                    // Send success ACK back to watch if batch ID is present
                    if (batchId != null) {
                        sendAck(batchId, success = true)
                    }
                } else {
                    Log.w(AppConfig.LogTags.PHONE_BLE, "No sensor data found in CSV")
                    // Still send ACK for empty but valid batch
                    if (batchId != null) {
                        sendAck(batchId, success = true)
                    }
                }
            } catch (e: Exception) {
                Log.e(AppConfig.LogTags.PHONE_BLE, "Error parsing or storing sensor data: ${e.message}", e)
                // Send failure ACK if we have a batch ID
                if (batchId != null) {
                    sendAck(batchId, success = false)
                }
            }
        }
    }
}
