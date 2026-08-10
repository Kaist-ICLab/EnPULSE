package kaist.iclab.mobiletracker.utils

import android.util.Log
import kaist.iclab.mobiletracker.config.AppConfig
import kaist.iclab.mobiletracker.data.DeviceType
import kaist.iclab.mobiletracker.db.entity.common.LocationEntity
import kaist.iclab.mobiletracker.db.entity.watch.AccelerometerEntity
import kaist.iclab.mobiletracker.db.entity.watch.ECGEntity
import kaist.iclab.mobiletracker.db.entity.watch.EDAEntity
import kaist.iclab.mobiletracker.db.entity.watch.HeartRateEntity
import kaist.iclab.mobiletracker.db.entity.watch.PPGEntity
import kaist.iclab.mobiletracker.db.entity.watch.SkinTemperatureEntity
import kaist.iclab.mobiletracker.db.entity.watch.WatchIMUEntity
import kaist.iclab.mobiletracker.db.entity.watch.WatchGestureEntity
import kaist.iclab.mobiletracker.db.entity.watch.WatchStressEntity

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

    fun parseLocationCsv(csvData: String): List<LocationEntity> =
        parseSensorSection(
            csvData = csvData,
            sectionName = "Location",
            headerPattern = "eventId,received,timestamp,latitude,longitude,altitude,speed,accuracy",
            rowParser = ::parseLocationRow
        )

    fun parseAccelerometerCsv(csvData: String): List<AccelerometerEntity> =
        parseSensorSection(
            csvData = csvData,
            sectionName = "Accelerometer",
            headerPattern = "eventId,received,timestamp,x,y,z",
            rowParser = ::parseAccelerometerRow
        )

    fun parsePPGCsv(csvData: String): List<PPGEntity> =
        parseSensorSection(
            csvData = csvData,
            sectionName = "PPG",
            headerPattern = "eventId,received,timestamp,green,greenStatus,red,redStatus,ir,irStatus",
            rowParser = ::parsePPGRow
        )

    fun parseHeartRateCsv(csvData: String): List<HeartRateEntity> =
        parseSensorSection(
            csvData = csvData,
            sectionName = "HeartRate",
            headerPattern = "eventId,received,timestamp,hr,hrStatus,ibi,ibiStatus",
            rowParser = ::parseHeartRateRow
        )

    fun parseSkinTemperatureCsv(csvData: String): List<SkinTemperatureEntity> =
        parseSensorSection(
            csvData = csvData,
            sectionName = "SkinTemperature",
            headerPattern = "eventId,received,timestamp,ambientTemp,objectTemp,status",
            rowParser = ::parseSkinTemperatureRow
        )

    fun parseEDACsv(csvData: String): List<EDAEntity> =
        parseSensorSection(
            csvData = csvData,
            sectionName = "EDA",
            headerPattern = "eventId,received,timestamp,skinConductance,status",
            rowParser = ::parseEDARow
        )

    fun parseECGCsv(csvData: String): List<ECGEntity> =
        parseSensorSection(
            csvData = csvData,
            sectionName = "ECG",
            headerPattern = "eventId,received,timestamp,ecgMv,leadOff,sequence,ppgGreen,maxThresholdMv,minThresholdMv",
            rowParser = ::parseECGRow
        )

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

                if (trimmedLine.replace(" ", "").equals(sectionName.replace(" ", ""), ignoreCase = true)) {
                    inSection = true
                    headerFound = false
                    continue
                }

                if (inSection && !headerFound) {
                    if (trimmedLine.contains(headerPattern, ignoreCase = true)) {
                        headerFound = true
                        continue
                    }
                }

                if (inSection && headerFound) {
                    if (trimmedLine.isNotEmpty() &&
                        !trimmedLine.first().isDigit() &&
                        !trimmedLine.first().isLetter().not() &&
                        !trimmedLine.replace(" ", "").equals(sectionName, ignoreCase = true) &&
                        isKnownSectionHeader(trimmedLine)
                    ) break

                    if (trimmedLine.isEmpty()) continue

                    rowParser(trimmedLine)?.let { dataList.add(it) }
                }
            }
        } catch (e: Exception) {
            Log.e(AppConfig.LogTags.PHONE_BLE, "Error parsing $sectionName CSV: ${e.message}", e)
        }

        return dataList
    }

    private fun isKnownSectionHeader(line: String): Boolean {
        val knownSections = listOf(
            "Accelerometer", "PPG", "HeartRate", "SkinTemperature",
            "EDA", "Location", "IMU", "Gesture", "Stress", "ECG"
        )
        val normalizedLine = line.replace(" ", "")
        return knownSections.any { normalizedLine.equals(it, ignoreCase = true) }
    }

    private fun parseLocationRow(row: String): LocationEntity? {
        return try {
            val parts = row.split(",").map { it.trim() }
            if (parts.size >= 8) {
                LocationEntity(
                    eventId = parts[0],
                    received = parts[1].toLongOrNull() ?: return null,
                    timestamp = parts[2].toLongOrNull() ?: return null,
                    deviceType = DeviceType.WATCH.value,
                    latitude = parts[3].toDoubleOrNull() ?: return null,
                    longitude = parts[4].toDoubleOrNull() ?: return null,
                    altitude = parts[5].toDoubleOrNull() ?: return null,
                    speed = parts[6].toFloatOrNull() ?: return null,
                    accuracy = parts[7].toFloatOrNull() ?: return null
                )
            } else null
        } catch (e: Exception) {
            Log.e(AppConfig.LogTags.PHONE_BLE, "Error parsing location row: ${e.message}", e)
            null
        }
    }

    private fun parseAccelerometerRow(row: String): AccelerometerEntity? {
        return try {
            val parts = row.split(",").map { it.trim() }
            if (parts.size >= 6) {
                AccelerometerEntity(
                    eventId = parts[0],
                    received = parts[1].toLongOrNull() ?: return null,
                    timestamp = parts[2].toLongOrNull() ?: return null,
                    x = parts[3].toFloatOrNull() ?: return null,
                    y = parts[4].toFloatOrNull() ?: return null,
                    z = parts[5].toFloatOrNull() ?: return null
                )
            } else null
        } catch (e: Exception) {
            Log.e(AppConfig.LogTags.PHONE_BLE, "Error parsing accelerometer row: ${e.message}", e)
            null
        }
    }

    private fun parsePPGRow(row: String): PPGEntity? {
        return try {
            val parts = row.split(",").map { it.trim() }
            if (parts.size >= 9) {
                PPGEntity(
                    eventId = parts[0],
                    received = parts[1].toLongOrNull() ?: return null,
                    timestamp = parts[2].toLongOrNull() ?: return null,
                    green = parts[3].toIntOrNull() ?: return null,
                    greenStatus = parts[4].toIntOrNull() ?: return null,
                    red = parts[5].toIntOrNull() ?: return null,
                    redStatus = parts[6].toIntOrNull() ?: return null,
                    ir = parts[7].toIntOrNull() ?: return null,
                    irStatus = parts[8].toIntOrNull() ?: return null
                )
            } else null
        } catch (e: Exception) {
            Log.e(AppConfig.LogTags.PHONE_BLE, "Error parsing PPG row: ${e.message}", e)
            null
        }
    }

    private fun parseECGRow(row: String): ECGEntity? {
        return try {
            val parts = row.split(",").map { it.trim() }
            if (parts.size >= 9) {
                ECGEntity(
                    eventId = parts[0],
                    received = parts[1].toLongOrNull() ?: return null,
                    timestamp = parts[2].toLongOrNull() ?: return null,
                    ecgMv = parts[3].toFloatOrNull() ?: return null,
                    leadOff = parts[4].toIntOrNull() ?: return null,
                    sequence = parts[5].toIntOrNull() ?: return null,
                    ppgGreen = parts[6].toIntOrNull() ?: return null,
                    maxThresholdMv = parts[7].toFloatOrNull() ?: return null,
                    minThresholdMv = parts[8].toFloatOrNull() ?: return null
                )
            } else null
        } catch (e: Exception) {
            Log.e(AppConfig.LogTags.PHONE_BLE, "Error parsing ECG row: ${e.message}", e)
            null
        }
    }

    private fun parseHeartRateRow(row: String): HeartRateEntity? {
        return try {
            val parts = row.split(",").map { it.trim() }
            if (parts.size >= 7) {
                HeartRateEntity(
                    eventId = parts[0],
                    received = parts[1].toLongOrNull() ?: return null,
                    timestamp = parts[2].toLongOrNull() ?: return null,
                    hr = parts[3].toIntOrNull() ?: return null,
                    hrStatus = parts[4].toIntOrNull() ?: return null,
                    ibi = parts[5].split(";").mapNotNull { it.trim().toIntOrNull() }.toIntArray(),
                    ibiStatus = parts[6].split(";").mapNotNull { it.trim().toIntOrNull() }.toIntArray()
                )
            } else null
        } catch (e: Exception) {
            Log.e(AppConfig.LogTags.PHONE_BLE, "Error parsing heart rate row: ${e.message}", e)
            null
        }
    }

    private fun parseSkinTemperatureRow(row: String): SkinTemperatureEntity? {
        return try {
            val parts = row.split(",").map { it.trim() }
            if (parts.size >= 6) {
                SkinTemperatureEntity(
                    eventId = parts[0],
                    received = parts[1].toLongOrNull() ?: return null,
                    timestamp = parts[2].toLongOrNull() ?: return null,
                    ambientTemp = parts[3].toFloatOrNull() ?: return null,
                    objectTemp = parts[4].toFloatOrNull() ?: return null,
                    status = parts[5].toIntOrNull() ?: return null
                )
            } else null
        } catch (e: Exception) {
            Log.e(AppConfig.LogTags.PHONE_BLE, "Error parsing skin temperature row: ${e.message}", e)
            null
        }
    }

    private fun parseEDARow(row: String): EDAEntity? {
        return try {
            val parts = row.split(",").map { it.trim() }
            if (parts.size >= 5) {
                EDAEntity(
                    eventId = parts[0],
                    received = parts[1].toLongOrNull() ?: return null,
                    timestamp = parts[2].toLongOrNull() ?: return null,
                    skinConductance = parts[3].toFloatOrNull() ?: return null,
                    status = parts[4].toIntOrNull() ?: return null
                )
            } else null
        } catch (e: Exception) {
            Log.e(AppConfig.LogTags.PHONE_BLE, "Error parsing EDA row: ${e.message}", e)
            null
        }
    }

    fun parseIMUCsv(csvData: String): List<WatchIMUEntity> {
        return parseSensorSection(
            csvData = csvData,
            sectionName = "IMU",
            headerPattern = "eventId,received,timestamp,accX,accY,accZ,gyroX,gyroY,gyroZ",
            rowParser = ::parseIMURow
        )
    }

    private fun parseIMURow(row: String): WatchIMUEntity? {
        return try {
            val parts = row.split(",").map { it.trim() }
            if (parts.size >= 9) {
                val eventId = parts[0]
                val received = parts[1].toLongOrNull() ?: return null
                val timestampMillis = parts[2].toLongOrNull() ?: return null
                val accX = parts[3].toFloatOrNull() ?: return null
                val accY = parts[4].toFloatOrNull() ?: return null
                val accZ = parts[5].toFloatOrNull() ?: return null
                val gyroX = parts[6].toFloatOrNull() ?: return null
                val gyroY = parts[7].toFloatOrNull() ?: return null
                val gyroZ = parts[8].toFloatOrNull() ?: return null

                WatchIMUEntity(
                    eventId = eventId,
                    uuid = "",
                    deviceType = DeviceType.WATCH.value,
                    timestamp = timestampMillis,
                    accX = accX,
                    accY = accY,
                    accZ = accZ,
                    gyroX = gyroX,
                    gyroY = gyroY,
                    gyroZ = gyroZ,
                    received = received,
                )
            } else null
        } catch (e: Exception) {
            Log.e(AppConfig.LogTags.PHONE_BLE, "Error parsing IMU row: ${e.message}", e)
            null
        }
    }

    fun parseGestureCsv(csvData: String): List<WatchGestureEntity> {
        return parseSensorSection(
            csvData = csvData,
            sectionName = "Gesture",
            headerPattern = "eventId,received,timestamp,classIndex,score,probabilities",
            rowParser = ::parseGestureRow
        )
    }

    private fun parseGestureRow(row: String): WatchGestureEntity? {
        return try {
            val parts = row.split(",").map { it.trim() }
            if (parts.size >= 6) {
                val eventId = parts[0]
                val received = parts[1].toLongOrNull() ?: return null
                val timestampMillis = parts[2].toLongOrNull() ?: return null
                val classIndex = parts[3].toIntOrNull() ?: return null
                val score = parts[4].toIntOrNull() ?: return null

                val probabilities = parts[5].split(";").mapNotNull { it.trim().toIntOrNull() }

                WatchGestureEntity(
                    eventId = eventId,
                    uuid = "",
                    deviceType = DeviceType.WATCH.value,
                    timestamp = timestampMillis,
                    classIndex = classIndex,
                    score = score,
                    probabilities = probabilities.toIntArray(),
                    received = received,
                )
            } else null
        } catch (e: Exception) {
            Log.e(AppConfig.LogTags.PHONE_BLE, "Error parsing Gesture row: ${e.message}", e)
            null
        }
    }

    fun parseStressCsv(csvData: String): List<WatchStressEntity> {
        return parseSensorSection(
            csvData = csvData,
            sectionName = "Stress",
            headerPattern = "eventId,received,timestamp,rmssd,ibiCount,threshold,isStressed",
            rowParser = ::parseStressRow
        )
    }

    private fun parseStressRow(row: String): WatchStressEntity? {
        return try {
            val parts = row.split(",").map { it.trim() }
            if (parts.size >= 9) {
                val eventId = parts[0]
                val received = parts[1].toLongOrNull() ?: return null
                val timestampMillis = parts[2].toLongOrNull() ?: return null
                val rmssd = parts[3].toFloatOrNull() ?: return null
                val ibiCount = parts[4].toIntOrNull() ?: return null
                val threshold = parts[5].toFloatOrNull() ?: return null
                val isStressed = parts[6].toBooleanStrictOrNull() ?: return null

                WatchStressEntity(
                    eventId = eventId,
                    uuid = "",
                    deviceType = DeviceType.WATCH.value,
                    timestamp = timestampMillis,
                    rmssd = rmssd,
                    ibiCount = ibiCount,
                    threshold = threshold,
                    isStressed = isStressed,
                    received = received,
                )
            } else null
        } catch (e: Exception) {
            Log.e(AppConfig.LogTags.PHONE_BLE, "Error parsing Stress row: ${e.message}", e)
            null
        }
    }
}
