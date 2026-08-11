package kaist.iclab.mobiletracker.services.upload

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kaist.iclab.mobiletracker.Constants
import kaist.iclab.mobiletracker.repository.onFailure
import kaist.iclab.mobiletracker.services.SyncTimestampService
import kaist.iclab.mobiletracker.utils.NetworkConditionChecker
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

/**
 * Flushes WebApp logs on a fixed ~1-minute cadence, independent of "Start Logging"/"Stop Logging"
 * — same intent as the old `AutoSyncService.startWebAppLogSync()` loop it replaces (see that
 * method's original doc comment: webapp logs aren't sensor data, so they drain on their own
 * schedule with no `getDataCollectionStarted()` gate). Self-reschedules like
 * [SensorAutoSyncWorker]; see that class's doc comment for why WorkManager over the old in-process
 * loop, and why this doesn't await [DataUploadService] — though in practice this worker doesn't
 * call DataUploadService at all, it flushes directly via [WebAppLogUploader].
 */
class WebAppLogSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    companion object {
        private const val TAG = "WebAppLogSyncWorker"

        fun scheduleNext(context: Context, delayMs: Long) {
            val request = OneTimeWorkRequestBuilder<WebAppLogSyncWorker>()
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                Constants.AutoSync.WORK_NAME_WEBAPP_LOG_SYNC,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(Constants.AutoSync.WORK_NAME_WEBAPP_LOG_SYNC)
        }
    }

    private val syncTimestampService: SyncTimestampService by inject()
    private val webAppLogUploader: WebAppLogUploader by inject()

    override suspend fun doWork(): Result {
        try {
            val networkMode = syncTimestampService.getAutoSyncNetworkMode()
            if (NetworkConditionChecker.isMet(applicationContext, networkMode)) {
                webAppLogUploader.flush().onFailure { e ->
                    Log.e(TAG, "WebApp log upload failed: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in webapp log sync cycle: ${e.message}", e)
        }

        scheduleNext(applicationContext, Constants.AutoSync.CHECK_INTERVAL_MS)
        return Result.success()
    }
}
