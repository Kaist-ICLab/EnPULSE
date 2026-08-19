package kaist.iclab.mobiletracker.repository

import android.util.Log
import io.github.jan.supabase.postgrest.from
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.repository.handlers.SensorDataHandlerRegistry
import kaist.iclab.mobiletracker.repository.handlers.SurveyResponseDataHandler
import kaist.iclab.mobiletracker.repository.handlers.WebAppLogDataHandler
import kaist.iclab.mobiletracker.services.SyncTimestampService
import kaist.iclab.mobiletracker.services.upload.SensorUploadService
import kaist.iclab.mobiletracker.services.upload.SurveyResponseUploader
import kaist.iclab.mobiletracker.services.upload.WebAppLogUploader
import kaist.iclab.mobiletracker.utils.toCampaignSensorName
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
    private val sensorUploadService: SensorUploadService,
    private val surveyResponseUploader: SurveyResponseUploader,
    private val webAppLogUploader: WebAppLogUploader,
    private val supabaseHelper: SupabaseHelper,
    private val campaignSensorRepository: CampaignSensorRepository
) : DataRepository {

    companion object {
        private const val TAG = "DataRepository"
    }

    private val syncingSensors = ConcurrentHashMap<String, Boolean>()

    /**
     * Survey and webapp logs are never campaign-gated — capture already ignores `campaign_table`
     * for both (see `PhoneSensorDataService.registerListeners` and [WebAppLogDataHandler]'s doc
     * comment), so the Data screen and manual upload must agree, or data the app happily captured
     * would look permanently invisible/unsendable.
     */
    private fun isSensorActive(sensorId: String): Boolean {
        if (sensorId == SurveyResponseDataHandler.SENSOR_ID || sensorId == WebAppLogDataHandler.SENSOR_ID) {
            return true
        }
        val activeSensorNames = campaignSensorRepository.getActiveSensors().map { it.name }
        return activeSensorNames.contains(sensorId.toCampaignSensorName())
    }

    override suspend fun getAllSensorInfo(): List<SensorInfo> =
        handlerRegistry.getAllHandlers()
            .filter { handler -> isSensorActive(handler.sensorId) }
            .map { handler ->
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
        if (syncingSensors.putIfAbsent(sensorId, true) != null) return -2
        try {
            // Survey and webapp logs don't ride SensorUploadService (insert-only, not
            // upsert-by-eventId; see their handlers' doc comments), so each gets its own uploader.
            if (sensorId == SurveyResponseDataHandler.SENSOR_ID) {
                return when (val result = surveyResponseUploader.flush()) {
                    is Result.Success -> if (result.data > 0) 1 else 0
                    is Result.Error -> -1
                }
            }
            if (sensorId == WebAppLogDataHandler.SENSOR_ID) {
                return when (val result = webAppLogUploader.flush()) {
                    is Result.Success -> if (result.data > 0) 1 else 0
                    is Result.Error -> -1
                }
            }

            if (!sensorUploadService.hasDataToUpload(sensorId)) return 0
            return when (sensorUploadService.uploadSensorData(sensorId)) {
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
        var failedCount = 0
        for (sensor in allSensors) {
            try {
                val result = uploadSensorData(sensor.sensorId)
                if (result > 0) {
                    successCount++
                } else if (result == -1) {
                    failedCount++
                }
                // result == 0 means no data for this sensor (skip)
                // result == -2 means already syncing (skip)
            } catch (e: Exception) {
                failedCount++
                Log.e(TAG, "Unexpected error uploading ${sensor.sensorId}", e)
            }
        }
        // Return positive for success count, negative if failures occurred but no successes
        return if (successCount > 0) successCount
        else if (failedCount > 0) -1
        else 0
    }

    override suspend fun deleteAllAllData() {
        val allSensors = getAllSensorInfo()
        for (sensor in allSensors) {
            try {
                deleteAllSensorData(sensor.sensorId)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting data for ${sensor.sensorId}", e)
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
                        eq(handler.remoteIdColumnName, eventId)
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
}
