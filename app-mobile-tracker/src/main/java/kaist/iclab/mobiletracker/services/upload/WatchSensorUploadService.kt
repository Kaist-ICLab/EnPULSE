package kaist.iclab.mobiletracker.services.upload

import android.util.Log
import io.github.jan.supabase.auth.auth
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.repository.CampaignSensorRepository
import kaist.iclab.mobiletracker.repository.Result
import kaist.iclab.mobiletracker.services.SyncTimestampService
import kaist.iclab.mobiletracker.services.upload.handlers.SensorUploadHandlerRegistry
import kaist.iclab.mobiletracker.utils.SupabaseSessionHelper
import kaist.iclab.mobiletracker.utils.toCampaignSensorName

/**
 * Service for uploading watch sensor data from Room database to Supabase.
 * Uses registry pattern to delegate upload operations to per-sensor handlers.
 *
 * Refactored from 234 lines to ~70 lines using SensorUploadHandlerRegistry.
 */
class WatchSensorUploadService(
    private val handlerRegistry: SensorUploadHandlerRegistry,
    private val supabaseHelper: SupabaseHelper,
    private val syncTimestampService: SyncTimestampService,
    private val campaignSensorRepository: CampaignSensorRepository
) {
    companion object {
        private const val TAG = "WatchSensorUploadService"
    }

    private fun isSensorActive(sensorId: String): Boolean {
        val activeSensors = campaignSensorRepository.getActiveSensors().map { it.name }
        val campaignSensorName = sensorId.toCampaignSensorName()
        return activeSensors.contains(campaignSensorName)
    }

    /**
     * Upload watch sensor data to Supabase.
     * @param sensorId The ID of the sensor to upload data for
     * @return Result indicating success or failure
     */
    suspend fun uploadSensorData(sensorId: String): Result<Unit> {
        if (!isSensorActive(sensorId)) return Result.Success(Unit)

        val handler = handlerRegistry.getHandler(sensorId)
        if (handler == null) {
            Log.w(TAG, "No upload handler found for watch sensor: $sensorId")
            return Result.Error(UnsupportedOperationException("Upload not implemented for watch sensor: $sensorId"))
        }

        val lastUploadTimestamp =
            syncTimestampService.getLastSuccessfulUploadTimestamp(sensorId) ?: 0L

        // Wait for Supabase Auth to finish loading the session from storage
        supabaseHelper.supabaseClient.auth.awaitInitialization()

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
            Log.e(TAG, "Error uploading watch sensor data for $sensorId: ${e.message}", e)
            Result.Error(e)
        }
    }

    /**
     * Check if there is data available to upload for a specific sensor.
     */
    suspend fun hasDataToUpload(sensorId: String): Boolean {
        if (!isSensorActive(sensorId)) return false

        return try {
            val handler = handlerRegistry.getHandler(sensorId) ?: return false
            val lastUploadTimestamp =
                syncTimestampService.getLastSuccessfulUploadTimestamp(sensorId) ?: 0L
            handler.hasDataToUpload(lastUploadTimestamp)
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Error checking data availability for watch sensor $sensorId: ${e.message}",
                e
            )
            false
        }
    }

    /**
     * Get the current user's UUID from session or cache.
     */
    private fun getUserUuid(): String? {
        var userUuid = SupabaseSessionHelper.getUuidOrNull(supabaseHelper.supabaseClient)

        if (userUuid.isNullOrEmpty()) {
            userUuid = syncTimestampService.getCachedUserUuid()
        }

        return userUuid?.takeIf { it.isNotEmpty() }
    }
}
