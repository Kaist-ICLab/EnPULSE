package kaist.iclab.mobiletracker.services.upload

import android.util.Log
import io.github.jan.supabase.auth.auth
import kaist.iclab.mobiletracker.Constants
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.repository.AppError
import kaist.iclab.mobiletracker.repository.CampaignSensorRepository
import kaist.iclab.mobiletracker.repository.Result
import kaist.iclab.mobiletracker.repository.SensorUploadOutcome
import kaist.iclab.mobiletracker.services.SyncTimestampService
import kaist.iclab.mobiletracker.services.upload.handlers.SensorUploadHandlerRegistry
import kaist.iclab.mobiletracker.utils.SupabaseSessionHelper
import kaist.iclab.mobiletracker.utils.toCampaignSensorName
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

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

    /** Whether [sensorId] is part of the currently joined campaign's active sensor set. */
    fun isSensorActive(sensorId: String): Boolean {
        val activeSensors = campaignSensorRepository.getActiveSensors().map { it.name }
        val campaignSensorName = sensorId.toCampaignSensorName()
        return activeSensors.contains(campaignSensorName)
    }

    /** Display name for [sensorId], falling back to the id when nothing is registered for it. */
    fun displayNameOf(sensorId: String): String =
        handlerRegistry.getHandler(sensorId)?.displayName ?: sensorId

    /**
     * Batches [uploadSensorData] would send for [sensorId] right now — used to size the upload
     * progress bar. 0 for sensors outside the campaign, unknown to the registry, or up to date.
     */
    suspend fun pendingBatchCount(sensorId: String): Int {
        if (!isSensorActive(sensorId)) return 0
        val handler = handlerRegistry.getHandler(sensorId) ?: return 0
        return handler.pendingBatchCount(syncTimestampService.getUploadCursor(sensorId))
    }

    /**
     * @param onBatchUploaded Invoked after each batch Supabase accepts, so callers can advance a
     * progress bar in batch-sized steps instead of jumping once per sensor.
     */
    suspend fun uploadSensorData(
        sensorId: String,
        onBatchUploaded: suspend () -> Unit = {}
    ): SensorUploadOutcome {
        if (!isSensorActive(sensorId)) {
            Log.d(TAG, "uploadSensorData: $sensorId is NOT active")
            return SensorUploadOutcome.UP_TO_DATE
        }

        val handler = handlerRegistry.getHandler(sensorId)
        if (handler == null) {
            Log.w(TAG, "No upload handler found for sensor: $sensorId")
            return SensorUploadOutcome(
                0, 0, isUpToDate = false,
                error = UnsupportedOperationException("Upload not implemented for sensor: $sensorId")
            )
        }

        val lastUploadCursor = syncTimestampService.getUploadCursor(sensorId)
        // The denominator of the success rate, measured before a single row is sent: everything
        // sitting in local storage that this run set out to upload. It cannot be derived after the
        // fact from how far the cursor moved — a run cut short by the network leaves the cursor
        // exactly where the last accepted batch put it, so "records the cursor passed" counts only
        // the successes and every interrupted upload would report a perfect 100%.
        val pendingBefore = handler.pendingRecordCount(lastUploadCursor)

        // Belt-and-suspenders: normally SupabaseHelper's enableLifecycleCallbacks = false keeps
        // SessionStatus from ever getting stuck at Initializing mid-upload. If it does anyway (any
        // other cause), don't let this sensor's upload wedge every sensor queued after it — fail
        // this one cleanly and let the caller move on, same as a real timed-out network call would.
        val initialized = withTimeoutOrNull(Constants.Network.AUTH_AWAIT_INITIALIZATION_TIMEOUT_MS.milliseconds) {
            supabaseHelper.supabaseClient.auth.awaitInitialization()
        } != null
        if (!initialized) {
            Log.e(TAG, "Timed out waiting for Supabase auth to initialize; skipping $sensorId")
            return SensorUploadOutcome(0, 0, isUpToDate = false, error = IllegalStateException("Auth session not ready"))
        }

        val userUuid = getUserUuid()
        if (userUuid == null) {
            Log.e(TAG, "Cannot upload data: No user UUID available")
            return SensorUploadOutcome(0, 0, isUpToDate = false, error = IllegalStateException("User not logged in"))
        }

        // Only start skipping a whole "poison" batch once the exact same spot has already failed
        // this way repeatedly across separate upload cycles (see
        // SyncTimestampService.recordUploadFailureAtCursor) — a single rejection is treated
        // normally first, in case it's transient-looking rather than a genuinely bad batch.
        val allowQuarantine = syncTimestampService.getUploadFailureStreak(sensorId) >=
            Constants.Network.QUARANTINE_AFTER_FAILED_UPLOAD_CYCLES

        // Counted per batch from what the handler actually sent, rather than inferred from the
        // cursor delta: the cursor is a row id, and deleted rows (deleteRecord / pruneData) leave
        // gaps in it, which would inflate the count.
        var succeededRecords = 0
        var quarantinedRecords = 0

        return try {
            // Persist the cursor as each batch lands rather than only once the sensor finishes, so
            // an error part-way through a large backlog keeps the batches that already reached
            // Supabase instead of re-uploading them on every retry.
            val result = handler.uploadData(
                userUuid,
                lastUploadCursor,
                allowQuarantine = allowQuarantine,
                onBatchUploaded = { cursor, recordCount ->
                    syncTimestampService.setUploadCursor(sensorId, cursor)
                    syncTimestampService.addUploadStats(sensorId, succeededBatches = 1)
                    succeededRecords += recordCount
                    onBatchUploaded()
                },
                onBatchQuarantined = { cursor, recordCount, reason ->
                    syncTimestampService.setUploadCursor(sensorId, cursor)
                    syncTimestampService.addUploadStats(sensorId, quarantinedRecordCount = recordCount)
                    quarantinedRecords += recordCount
                    Log.w(TAG, "Gave up on $recordCount $sensorId record(s) up to cursor $cursor: $reason")
                    onBatchUploaded()
                }
            )

            val cursorAfter = syncTimestampService.getUploadCursor(sensorId)
            val succeeded = succeededRecords
            // What this run took on. Normally that's the backlog measured up front, but sensors keep
            // collecting while the upload runs and each pass drains until the store is empty, so a
            // long run can legitimately get through more than it started with — hence the max,
            // which also keeps the rate from ever exceeding 100%.
            val attempted = maxOf(pendingBefore, succeeded + quarantinedRecords)

            when (result) {
                is Result.Success -> {
                    syncTimestampService.updateLastSuccessfulUpload(sensorId)
                    syncTimestampService.clearUploadFailureStreak(sensorId)

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

                    SensorUploadOutcome(succeeded, attempted, isUpToDate = false)
                }
                is Result.Error -> {
                    // Only a decisive server rejection counts toward the quarantine streak — a
                    // network/timeout/auth-not-ready failure says nothing about whether this data
                    // is actually the problem, so it must not push a sensor closer to giving up on
                    // real data over what might just be the network being briefly unavailable.
                    if (result.exception is AppError.ServerRejected) {
                        // onBatchUploaded already persisted the cursor for whatever got through
                        // before the failing batch, so this read reflects exactly where it's stuck.
                        val streak = syncTimestampService.recordUploadFailureAtCursor(sensorId, cursorAfter)
                        Log.w(
                            TAG,
                            "$sensorId upload rejected by server (cycle $streak/" +
                                "${Constants.Network.QUARANTINE_AFTER_FAILED_UPLOAD_CYCLES} at this spot)"
                        )
                    }
                    SensorUploadOutcome(succeeded, attempted, isUpToDate = false, error = result.exception)
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Error uploading sensor data for $sensorId: ${e.message}", e)
            // Same accounting as the Result.Error path above — batches that landed before this
            // threw still count, so the summary reports how far it got rather than a flat zero.
            SensorUploadOutcome(
                succeededRecords,
                maxOf(pendingBefore, succeededRecords + quarantinedRecords),
                isUpToDate = false,
                error = e
            )
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
