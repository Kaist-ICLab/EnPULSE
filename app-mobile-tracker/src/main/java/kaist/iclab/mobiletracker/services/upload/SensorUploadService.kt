package kaist.iclab.mobiletracker.services.upload

import kotlinx.coroutines.CancellationException
import android.util.Log
import io.github.jan.supabase.auth.auth
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.repository.CampaignSensorRepository
import kaist.iclab.mobiletracker.repository.Result
import kaist.iclab.mobiletracker.services.SyncTimestampService
import kaist.iclab.mobiletracker.services.upload.handlers.SensorUploadHandlerRegistry
import kaist.iclab.mobiletracker.utils.SupabaseSessionHelper
import kaist.iclab.mobiletracker.utils.toCampaignSensorName

class SensorUploadService(
    private val handlerRegistry: SensorUploadHandlerRegistry,
    private val supabaseHelper: SupabaseHelper,
    private val syncTimestampService: SyncTimestampService,
    private val campaignSensorRepository: CampaignSensorRepository
) {
    companion object {
        private const val TAG = "SensorUploadService"

        /** Buffer to keep synced data locally (7 days) */
        private const val PRUNE_BUFFER_MS = 7 * 24 * 60 * 60 * 1000L
    }

    private fun isSensorActive(sensorId: String): Boolean {
        val activeSensors = campaignSensorRepository.getActiveSensors().map { it.name }
        val campaignSensorName = sensorId.toCampaignSensorName()
        return activeSensors.contains(campaignSensorName)
    }

    suspend fun uploadSensorData(sensorId: String): Result<Unit> {
        if (!isSensorActive(sensorId)) {
            Log.d(TAG, "uploadSensorData: $sensorId is NOT active")
            return Result.Success(Unit)
        }

        val handler = handlerRegistry.getHandler(sensorId)
        if (handler == null) {
            Log.w(TAG, "No upload handler found for sensor: $sensorId")
            return Result.Error(UnsupportedOperationException("Upload not implemented for sensor: $sensorId"))
        }

        val lastUploadCursor = syncTimestampService.getUploadCursor(sensorId)

        supabaseHelper.supabaseClient.auth.awaitInitialization()

        val userUuid = getUserUuid()
        if (userUuid == null) {
            Log.e(TAG, "Cannot upload data: No user UUID available")
            return Result.Error(IllegalStateException("User not logged in"))
        }

        return try {
            when (val result = handler.uploadData(userUuid, lastUploadCursor)) {
                is Result.Success -> {
                    syncTimestampService.setUploadCursor(sensorId, result.data)
                    syncTimestampService.updateLastSuccessfulUpload(sensorId)

                    // Prune data that is BOTH synced AND older than PRUNE_BUFFER_MS
                    // NOTE: This is currently disabled as per user preference for infinite local retention.
                    // Infrastructure is kept for future manual activation. If re-enabled: result.data
                    // is now the upload cursor (a local row id), not a timestamp — pruneData() takes a
                    // timestamp threshold, so this needs a real "max uploaded timestamp" from the
                    // handler again (e.g. have uploadData() report both the new cursor and the max
                    // event timestamp of the batch) before this can be un-commented as-is.
                    /*
                    val pruneThreshold = minOf(
                        result.data,
                        System.currentTimeMillis() - PRUNE_BUFFER_MS
                    )
                    handler.pruneData(pruneThreshold)
                    */

                    Result.Success(Unit)
                }
                is Result.Error -> result
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Error uploading sensor data for $sensorId: ${e.message}", e)
            Result.Error(e)
        }
    }

    suspend fun hasDataToUpload(sensorId: String): Boolean {
        if (!isSensorActive(sensorId)) {
            Log.d(TAG, "hasDataToUpload: $sensorId is NOT active")
            return false
        }
        return try {
            val handler = handlerRegistry.getHandler(sensorId)
            if (handler == null) {
                Log.d(TAG, "hasDataToUpload: No handler for $sensorId")
                return false
            }
            val lastUploadCursor = syncTimestampService.getUploadCursor(sensorId)
            val hasData = handler.hasDataToUpload(lastUploadCursor)
            Log.d(
                TAG,
                "hasDataToUpload: $sensorId hasData=$hasData (cursor=$lastUploadCursor)"
            )
            hasData
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Error checking data availability for sensor $sensorId: ${e.message}", e)
            false
        }
    }

    private fun getUserUuid(): String? {
        var userUuid = SupabaseSessionHelper.getUuidOrNull(supabaseHelper.supabaseClient)
        if (userUuid.isNullOrEmpty()) {
            userUuid = syncTimestampService.getCachedUserUuid()
        }
        return userUuid?.takeIf { it.isNotEmpty() }
    }
}
