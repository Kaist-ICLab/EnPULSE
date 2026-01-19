package kaist.iclab.mobiletracker.services.upload

import android.util.Log
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.repository.Result
import kaist.iclab.mobiletracker.services.SyncTimestampService
import kaist.iclab.mobiletracker.services.upload.handlers.SensorUploadHandlerRegistry
import kaist.iclab.mobiletracker.utils.SupabaseSessionHelper

/**
 * Service for uploading phone sensor data from Room database to Supabase.
 * Uses registry pattern to delegate upload operations to per-sensor handlers.
 * 
 * Refactored from 432 lines to ~80 lines using SensorUploadHandlerRegistry.
 */
class PhoneSensorUploadService(
    private val handlerRegistry: SensorUploadHandlerRegistry,
    private val supabaseHelper: SupabaseHelper,
    private val syncTimestampService: SyncTimestampService
) {
    companion object {
        private const val TAG = "PhoneSensorUploadService"
    }

    /**
     * Upload sensor data to Supabase.
     * @param sensorId The ID of the sensor to upload data for
     * @return Result indicating success or failure
     */
    suspend fun uploadSensorData(sensorId: String): Result<Unit> {
        val handler = handlerRegistry.getHandler(sensorId)
        if (handler == null) {
            Log.w(TAG, "No upload handler found for sensor: $sensorId")
            return Result.Error(UnsupportedOperationException("Upload not implemented for sensor: $sensorId"))
        }

        val lastUploadTimestamp = syncTimestampService.getLastSuccessfulUploadTimestamp(sensorId) ?: 0L
        
        val userUuid = getUserUuid()
        if (userUuid == null) {
            Log.e(TAG, "Cannot upload data: No user UUID available")
            return Result.Error(IllegalStateException("User not logged in"))
        }

        return try {
            when (val result = handler.uploadData(userUuid, lastUploadTimestamp)) {
                is Result.Success -> {
                    syncTimestampService.updateLastSuccessfulUpload(sensorId, result.data)
                    Result.Success(Unit)
                }
                is Result.Error -> result
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading sensor data for $sensorId: ${e.message}", e)
            Result.Error(e)
        }
    }

    /**
     * Check if there is data available to upload for a specific sensor.
     */
    suspend fun hasDataToUpload(sensorId: String): Boolean {
        return try {
            val handler = handlerRegistry.getHandler(sensorId) ?: return false
            val lastUploadTimestamp = syncTimestampService.getLastSuccessfulUploadTimestamp(sensorId) ?: 0L
            handler.hasDataToUpload(lastUploadTimestamp)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking data availability for sensor $sensorId: ${e.message}", e)
            false
        }
    }

    /**
     * Get the current user's UUID from session or cache.
     */
    private fun getUserUuid(): String? {
        // Try to get UUID from current session first (most reliable)
        var userUuid = SupabaseSessionHelper.getUuidOrNull(supabaseHelper.supabaseClient)
        
        // If session not available (e.g., app in background), use cached UUID
        if (userUuid.isNullOrEmpty()) {
            userUuid = syncTimestampService.getCachedUserUuid()
        }
        
        return userUuid?.takeIf { it.isNotEmpty() }
    }
}
