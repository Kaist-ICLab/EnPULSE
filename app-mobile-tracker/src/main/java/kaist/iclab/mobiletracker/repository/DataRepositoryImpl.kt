package kaist.iclab.mobiletracker.repository

import android.util.Log
import io.github.jan.supabase.postgrest.from
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.repository.handlers.SensorDataHandlerRegistry
import kaist.iclab.mobiletracker.services.SyncTimestampService
import kaist.iclab.mobiletracker.services.upload.PhoneSensorUploadService
import kaist.iclab.mobiletracker.services.upload.WatchSensorUploadService
import kaist.iclab.mobiletracker.utils.SensorTypeHelper
import java.util.concurrent.ConcurrentHashMap

/**
 * Implementation of DataRepository that uses SensorDataHandlerRegistry
 * to delegate per-sensor operations to individual handlers.
 * 
 * This refactored version eliminates repetitive when-expressions by using
 * the registry pattern for sensor-specific operations.
 */
class DataRepositoryImpl(
    private val handlerRegistry: SensorDataHandlerRegistry,
    private val syncTimestampService: SyncTimestampService,
    private val phoneSensorUploadService: PhoneSensorUploadService,
    private val watchSensorUploadService: WatchSensorUploadService,
    private val supabaseHelper: SupabaseHelper
) : DataRepository {
    
    private val syncingSensors = ConcurrentHashMap<String, Boolean>()

    override suspend fun getAllSensorInfo(): List<SensorInfo> =
        handlerRegistry.getAllHandlers().map { handler ->
            SensorInfo(
                sensorId = handler.sensorId,
                displayName = handler.displayName,
                recordCount = handler.getRecordCount(),
                lastRecordedTime = handler.getLatestTimestamp(),
                isWatchSensor = handler.isWatchSensor || handler.sensorId == "Location",
                isPhoneSensor = handler.isPhoneSensor
            )
        }.sortedBy { it.displayName }

    override suspend fun getSensorInfo(sensorId: String): SensorInfo? =
        getAllSensorInfo().find { it.sensorId == sensorId }

    override suspend fun getSensorDetailInfo(sensorId: String): SensorDetailInfo? {
        val handler = handlerRegistry.getHandler(sensorId) ?: return null
        val startOfToday = getStartOfToday()
        
        return SensorDetailInfo(
            sensorId = handler.sensorId,
            displayName = handler.displayName,
            totalRecords = handler.getRecordCount(),
            todayRecords = handler.getRecordCountAfterTimestamp(startOfToday),
            lastRecordedTime = handler.getLatestTimestamp(),
            lastSyncTimestamp = syncTimestampService.getLastSuccessfulUploadTimestamp(sensorId),
            isWatchSensor = handler.isWatchSensor || handler.sensorId == "Location",
            isPhoneSensor = handler.isPhoneSensor
        )
    }

    override suspend fun getSensorRecords(
        sensorId: String,
        dateFilter: DateFilter,
        sortOrder: SortOrder,
        limit: Int,
        offset: Int
    ): List<SensorRecord> {
        val handler = handlerRegistry.getHandler(sensorId) ?: return emptyList()
        val afterTimestamp = getTimestampForFilter(dateFilter)
        val isAscending = sortOrder == SortOrder.OLDEST_FIRST
        return handler.getRecordsPaginated(afterTimestamp, isAscending, limit, offset)
    }

    override suspend fun getSensorRecordCount(sensorId: String, dateFilter: DateFilter): Int {
        val handler = handlerRegistry.getHandler(sensorId) ?: return 0
        val afterTimestamp = getTimestampForFilter(dateFilter)
        return handler.getRecordCountAfterTimestamp(afterTimestamp)
    }

    override suspend fun deleteAllSensorData(sensorId: String) {
        val handler = handlerRegistry.getHandler(sensorId) ?: return
        handler.deleteAll()
        syncTimestampService.clearLastSuccessfulUpload(sensorId)
    }

    override suspend fun uploadSensorData(sensorId: String): Int {
        if (syncingSensors.putIfAbsent(sensorId, true) != null) {
            return -2 // Already syncing
        }
        
        try {
            // Special handling for Location: upload both phone and watch data
            if (sensorId == "Location") {
                var result = 0
                
                // 1. Upload Phone Location
                if (phoneSensorUploadService.hasDataToUpload("Location")) {
                    when (phoneSensorUploadService.uploadSensorData("Location")) {
                        is Result.Success -> result = 1
                        is Result.Error -> return -1 // Immediate failure on error
                    }
                }
                
                // 2. Upload Watch Location (stored with ID "WatchLocation")
                if (watchSensorUploadService.hasDataToUpload("WatchLocation")) {
                    when (watchSensorUploadService.uploadSensorData("WatchLocation")) {
                        is Result.Success -> result = 1
                        is Result.Error -> return -1 // Immediate failure on error
                    }
                }
                
                return result
            }

            if (SensorTypeHelper.isWatchSensor(sensorId)) {
                if (!watchSensorUploadService.hasDataToUpload(sensorId)) {
                    return 0
                }
                return when (watchSensorUploadService.uploadSensorData(sensorId)) {
                    is Result.Success -> 1
                    is Result.Error -> -1
                }
            }
            
            if (!phoneSensorUploadService.hasDataToUpload(sensorId)) {
                return 0
            }
            return when (phoneSensorUploadService.uploadSensorData(sensorId)) {
                is Result.Success -> 1
                is Result.Error -> -1
            }
        } finally {
            syncingSensors.remove(sensorId)
        }
    }

    override suspend fun getLastSyncTimestamp(sensorId: String): Long? =
        syncTimestampService.getLastSuccessfulUploadTimestamp(sensorId)

    override suspend fun uploadAllData(): Int {
        val allSensors = getAllSensorInfo()
        var successCount = 0
        for (sensor in allSensors) {
            try {
                val result = uploadSensorData(sensor.sensorId)
                if (result > 0) {
                    successCount++
                }
            } catch (e: Exception) {
                // Continue to next sensor
            }
        }
        return successCount
    }

    override suspend fun deleteAllAllData() {
        val allSensors = getAllSensorInfo()
        for (sensor in allSensors) {
            try {
                deleteAllSensorData(sensor.sensorId)
            } catch (e: Exception) {
                // Continue to next sensor
            }
        }
    }

    override suspend fun deleteRecord(sensorId: String, recordId: Long) {
        val handler = handlerRegistry.getHandler(sensorId) ?: return
        
        // Get eventId before deleting locally (needed for Supabase deletion)
        val eventId = handler.getEventIdById(recordId)
        
        // Delete from local database first
        handler.deleteById(recordId)
        
        // Try to delete from Supabase if eventId exists
        if (eventId != null) {
            try {
                supabaseHelper.supabaseClient.from(handler.supabaseTableName).delete {
                    filter {
                        eq("event_id", eventId)
                    }
                }
                Log.d("DataRepositoryImpl", "Deleted record from Supabase: $eventId")
            } catch (e: Exception) {
                // Log error but don't fail - local deletion already succeeded
                Log.e("DataRepositoryImpl", "Failed to delete from Supabase: ${e.message}")
            }
        }
    }

    private fun getStartOfToday(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getTimestampForFilter(dateFilter: DateFilter): Long {
        val calendar = java.util.Calendar.getInstance()
        return when (dateFilter) {
            DateFilter.TODAY -> {
                calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                calendar.set(java.util.Calendar.MINUTE, 0)
                calendar.set(java.util.Calendar.SECOND, 0)
                calendar.set(java.util.Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            DateFilter.LAST_7_DAYS -> {
                calendar.add(java.util.Calendar.DAY_OF_YEAR, -7)
                calendar.timeInMillis
            }
            DateFilter.LAST_30_DAYS -> {
                calendar.add(java.util.Calendar.DAY_OF_YEAR, -30)
                calendar.timeInMillis
            }
            DateFilter.ALL_TIME -> 0L
            DateFilter.CUSTOM -> 0L // Custom range handled separately
        }
    }
    
    override suspend fun getAllSensorRecordsForExport(sensorId: String, dateFilter: DateFilter): List<SensorRecord> {
        val handler = handlerRegistry.getHandler(sensorId) ?: return emptyList()
        val afterTimestamp = getTimestampForFilter(dateFilter)
        
        // Get all records without pagination (use a very large limit)
        return handler.getRecordsPaginated(
            afterTimestamp = afterTimestamp,
            isAscending = false,
            limit = Int.MAX_VALUE,
            offset = 0
        )
    }
}
