package kaist.iclab.mobiletracker.utils

import java.time.Instant
import java.util.UUID
import android.util.Log
import kaist.iclab.mobiletracker.config.AppConfig
import kaist.iclab.mobiletracker.data.DeviceType
import kaist.iclab.mobiletracker.data.sensors.watch.AccelerometerSensorData
import kaist.iclab.mobiletracker.data.sensors.watch.EDASensorData
import kaist.iclab.mobiletracker.data.sensors.watch.HeartRateSensorData
import kaist.iclab.mobiletracker.data.sensors.common.LocationSensorData
import kaist.iclab.mobiletracker.data.sensors.watch.PPGSensorData
import kaist.iclab.mobiletracker.data.sensors.watch.SkinTemperatureSensorData

/**
 * Parser for sensor data in CSV format received from wearable devices.
 * 
 * Supports parsing multiple sensor types from a single CSV string:
 * - accelerometer: eventId,received,timestamp,x,y,z
 * - ppg: eventId,received,timestamp,green,greenStatus,red,redStatus,ir,irStatus
 * - heartRate: eventId,received,timestamp,hr,hrStatus,ibi,ibiStatus (ibi and ibiStatus are semicolon-separated lists)
 * - skinTemperature: eventId,received,timestamp,ambientTemp,objectTemp,status
 * - eda: eventId,received,timestamp,skinConductance,status
 * - location: eventId,received,timestamp,latitude,longitude,altitude,speed,accuracy
 */
object SensorDataCsvParser {
    
    /**
     * Parse CSV data to extract location sensor entries
     * 
     * @param csvData The raw CSV string containing sensor data
     * @return List of parsed LocationSensorData, empty list if parsing fails
     */
    fun parseLocationCsv(csvData: String): List<LocationSensorData> {
        return parseSensorSection(
            csvData = csvData,
            sectionName = "Location",
            headerPattern = "eventId,received,timestamp,latitude,longitude,altitude,speed,accuracy",
            rowParser = ::parseLocationRow
        )
    }

    /**
     * Parse a single location data row
     * Format: eventId,received,timestamp,latitude,longitude,altitude,speed,accuracy
     */
    private fun parseLocationRow(row: String): LocationSensorData? {
        return try {
            val parts = row.split(",").map { it.trim() }
            if (parts.size >= 8) {
                val eventId = parts[0]
                val received = parts[1].toLongOrNull() ?: return null
                val timestampMillis = parts[2].toLongOrNull() ?: return null
                val latitude = parts[3].toDoubleOrNull() ?: return null
                val longitude = parts[4].toDoubleOrNull() ?: return null
                val altitude = parts[5].toDoubleOrNull() ?: return null
                val speed = parts[6].toFloatOrNull() ?: return null
                val accuracy = parts[7].toFloatOrNull() ?: return null
                
                LocationSensorData(
                    eventId = eventId,
                    uuid = null,
                    deviceType = DeviceType.WATCH.value,
                    timestamp = Instant.ofEpochMilli(timestampMillis).toString(),
                    accuracy = accuracy,
                    altitude = altitude,
                    latitude = latitude,
                    longitude = longitude,
                    speed = speed,
                    received = Instant.ofEpochMilli(received).toString()
                )
            } else null
        } catch (e: Exception) {
            Log.e(AppConfig.LogTags.PHONE_BLE, "Error parsing location row: ${e.message}", e)
            null
        }
    }

    /**
     * Parse CSV data to extract accelerometer sensor entries
     * CSV format: accelerometer\neventId,received,timestamp,x,y,z\n...
     */
    fun parseAccelerometerCsv(csvData: String): List<AccelerometerSensorData> {
        return parseSensorSection(
            csvData = csvData,
            sectionName = "Accelerometer",
            headerPattern = "eventId,received,timestamp,x,y,z",
            rowParser = ::parseAccelerometerRow
        )
    }

    /**
     * Parse CSV data to extract PPG sensor entries
     * CSV format: ppg\neventId,received,timestamp,green,greenStatus,red,redStatus,ir,irStatus\n...
     */
    fun parsePPGCsv(csvData: String): List<PPGSensorData> {
        return parseSensorSection(
            csvData = csvData,
            sectionName = "PPG",
            headerPattern = "eventId,received,timestamp,green,greenStatus,red,redStatus,ir,irStatus",
            rowParser = ::parsePPGRow
        )
    }

    /**
     * Parse CSV data to extract heart rate sensor entries
     * CSV format: heartRate\neventId,received,timestamp,hr,hrStatus,ibi,ibiStatus\n...
     * Note: ibi and ibiStatus are semicolon-separated lists
     */
    fun parseHeartRateCsv(csvData: String): List<HeartRateSensorData> {
        return parseSensorSection(
            csvData = csvData,
            sectionName = "HeartRate",
            headerPattern = "eventId,received,timestamp,hr,hrStatus,ibi,ibiStatus",
            rowParser = ::parseHeartRateRow
        )
    }

    /**
     * Parse CSV data to extract skin temperature sensor entries
     * CSV format: skinTemperature\neventId,received,timestamp,ambientTemp,objectTemp,status\n...
     */
    fun parseSkinTemperatureCsv(csvData: String): List<SkinTemperatureSensorData> {
        return parseSensorSection(
            csvData = csvData,
            sectionName = "SkinTemperature",
            headerPattern = "eventId,received,timestamp,ambientTemp,objectTemp,status",
            rowParser = ::parseSkinTemperatureRow
        )
    }

    /**
     * Parse CSV data to extract EDA sensor entries
     * CSV format: eda\neventId,received,timestamp,skinConductance,status\n...
     */
    fun parseEDACsv(csvData: String): List<EDASensorData> {
        return parseSensorSection(
            csvData = csvData,
            sectionName = "EDA",
            headerPattern = "eventId,received,timestamp,skinConductance,status",
            rowParser = ::parseEDARow
        )
    }

    /**
     * Generic parser for sensor sections in CSV
     */
    private fun <T> parseSensorSection(
        csvData: String,
        sectionName: String,
        headerPattern: String,
        rowParser: (String) -> T?
    ): List<T> {
        val dataList = mutableListOf<T>()
        
        try {
            val lines = csvData.lines()
            var inSection = false
            var headerFound = false
            
            for (line in lines) {
                val trimmedLine = line.trim()
                
                // Check if we're entering the section
                if (trimmedLine.replace(" ", "").equals(sectionName.replace(" ", ""), ignoreCase = true)) {
                    inSection = true
                    headerFound = false
                    continue
                }
                
                // If we're in section, look for header
                if (inSection && !headerFound) {
                    if (trimmedLine.contains(headerPattern, ignoreCase = true)) {
                        headerFound = true
                        continue
                    }
                }
                
                // If header found, parse data rows
                if (inSection && headerFound) {
                    // Check if we've moved to a new section
                    if (trimmedLine.isNotEmpty() && 
                        !trimmedLine.first().isDigit() && 
                        !trimmedLine.first().isLetter().not() &&
                        !trimmedLine.replace(" ", "").equals(sectionName, ignoreCase = true) &&
                        isKnownSectionHeader(trimmedLine)) {
                        break
                    }
                    
                    // Skip empty lines
                    if (trimmedLine.isEmpty()) {
                        continue
                    }
                    
                    // Parse data row
                    val data = rowParser(trimmedLine)
                    data?.let { dataList.add(it) }
                }
            }
        } catch (e: Exception) {
            Log.e(AppConfig.LogTags.PHONE_BLE, "Error parsing $sectionName CSV: ${e.message}", e)
        }
        
        return dataList
    }

    /**
     * Check if a line is a known section header
     */
    private fun isKnownSectionHeader(line: String): Boolean {
        val knownSections = listOf(
            "Accelerometer", "PPG", "HeartRate", "SkinTemperature", 
            "EDA", "Location"
        )
        val normalizedLine = line.replace(" ", "")
        return knownSections.any { normalizedLine.equals(it, ignoreCase = true) }
    }

    /**
     * Parse a single accelerometer data row
     * Format: eventId,received,timestamp,x,y,z
     */
    private fun parseAccelerometerRow(row: String): AccelerometerSensorData? {
        return try {
            val parts = row.split(",").map { it.trim() }
            if (parts.size >= 6) {
                val eventId = parts[0]
                val received = parts[1].toLongOrNull() ?: return null
                val timestampMillis = parts[2].toLongOrNull() ?: return null
                val x = parts[3].toFloatOrNull() ?: return null
                val y = parts[4].toFloatOrNull() ?: return null
                val z = parts[5].toFloatOrNull() ?: return null
                
                AccelerometerSensorData(
                    eventId = eventId,
                    uuid = null,
                    deviceType = DeviceType.WATCH.value,
                    timestamp = Instant.ofEpochMilli(timestampMillis).toString(),
                    x = x,
                    y = y,
                    z = z,
                    received = Instant.ofEpochMilli(received).toString(),
                )
            } else null
        } catch (e: Exception) {
            Log.e(AppConfig.LogTags.PHONE_BLE, "Error parsing accelerometer row: ${e.message}", e)
            null
        }
    }

    /**
     * Parse a single PPG data row
     * Format: eventId,received,timestamp,green,greenStatus,red,redStatus,ir,irStatus
     */
    private fun parsePPGRow(row: String): PPGSensorData? {
        return try {
            val parts = row.split(",").map { it.trim() }
            if (parts.size >= 9) {
                val eventId = parts[0]
                val received = parts[1].toLongOrNull() ?: return null
                val timestampMillis = parts[2].toLongOrNull() ?: return null
                val green = parts[3].toIntOrNull() ?: return null
                val greenStatus = parts[4].toIntOrNull() ?: return null
                val red = parts[5].toIntOrNull() ?: return null
                val redStatus = parts[6].toIntOrNull() ?: return null
                val ir = parts[7].toIntOrNull() ?: return null
                val irStatus = parts[8].toIntOrNull() ?: return null
                
                PPGSensorData(
                    eventId = eventId,
                    uuid = null,
                    deviceType = DeviceType.WATCH.value,
                    timestamp = Instant.ofEpochMilli(timestampMillis).toString(),
                    green = green,
                    greenStatus = greenStatus,
                    red = red,
                    redStatus = redStatus,
                    ir = ir,
                    irStatus = irStatus,
                    received = Instant.ofEpochMilli(received).toString(),
                )
            } else null
        } catch (e: Exception) {
            Log.e(AppConfig.LogTags.PHONE_BLE, "Error parsing PPG row: ${e.message}", e)
            null
        }
    }

    /**
     * Parse a single heart rate data row
     * Format: eventId,received,timestamp,hr,hrStatus,ibi,ibiStatus
     * Note: ibi and ibiStatus are semicolon-separated lists
     */
    private fun parseHeartRateRow(row: String): HeartRateSensorData? {
        return try {
            val parts = row.split(",").map { it.trim() }
            if (parts.size >= 7) {
                val eventId = parts[0]
                val received = parts[1].toLongOrNull() ?: return null
                val timestampMillis = parts[2].toLongOrNull() ?: return null
                val hr = parts[3].toIntOrNull() ?: return null
                val hrStatus = parts[4].toIntOrNull() ?: return null
                
                // Parse semicolon-separated lists
                val ibi = parts[5].split(";").mapNotNull { it.trim().toIntOrNull() }
                val ibiStatus = parts[6].split(";").mapNotNull { it.trim().toIntOrNull() }
                
                HeartRateSensorData(
                    eventId = eventId,
                    uuid = null,
                    deviceType = DeviceType.WATCH.value,
                    timestamp = Instant.ofEpochMilli(timestampMillis).toString(),
                    hr = hr,
                    hrStatus = hrStatus,
                    ibi = ibi,
                    ibiStatus = ibiStatus,
                    received = Instant.ofEpochMilli(received).toString(),
                )
            } else null
        } catch (e: Exception) {
            Log.e(AppConfig.LogTags.PHONE_BLE, "Error parsing heart rate row: ${e.message}", e)
            null
        }
    }

    /**
     * Parse a single skin temperature data row
     * Format: eventId,received,timestamp,ambientTemp,objectTemp,status
     */
    private fun parseSkinTemperatureRow(row: String): SkinTemperatureSensorData? {
        return try {
            val parts = row.split(",").map { it.trim() }
            if (parts.size >= 6) {
                val eventId = parts[0]
                val received = parts[1].toLongOrNull() ?: return null
                val timestampMillis = parts[2].toLongOrNull() ?: return null
                val ambientTemp = parts[3].toFloatOrNull() ?: return null
                val objectTemp = parts[4].toFloatOrNull() ?: return null
                val status = parts[5].toIntOrNull() ?: return null
                
                SkinTemperatureSensorData(
                    eventId = eventId,
                    uuid = null,
                    deviceType = DeviceType.WATCH.value,
                    timestamp = Instant.ofEpochMilli(timestampMillis).toString(),
                    ambientTemp = ambientTemp,
                    objectTemp = objectTemp,
                    status = status,
                    received = Instant.ofEpochMilli(received).toString(),
                )
            } else null
        } catch (e: Exception) {
            Log.e(AppConfig.LogTags.PHONE_BLE, "Error parsing skin temperature row: ${e.message}", e)
            null
        }
    }

    /**
     * Parse a single EDA data row
     * Format: eventId,received,timestamp,skinConductance,status
     */
    private fun parseEDARow(row: String): EDASensorData? {
        return try {
            val parts = row.split(",").map { it.trim() }
            if (parts.size >= 5) {
                val eventId = parts[0]
                val received = parts[1].toLongOrNull() ?: return null
                val timestampMillis = parts[2].toLongOrNull() ?: return null
                val skinConductance = parts[3].toFloatOrNull() ?: return null
                val status = parts[4].toIntOrNull() ?: return null
                
                EDASensorData(
                    eventId = eventId,
                    uuid = null,
                    deviceType = DeviceType.WATCH.value,
                    timestamp = Instant.ofEpochMilli(timestampMillis).toString(),
                    skinConductance = skinConductance,
                    status = status,
                    received = Instant.ofEpochMilli(received).toString(),
                )
            } else null
        } catch (e: Exception) {
            Log.e(AppConfig.LogTags.PHONE_BLE, "Error parsing EDA row: ${e.message}", e)
            null
        }
    }
}
