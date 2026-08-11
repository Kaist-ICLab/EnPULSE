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
import kaist.iclab.mobiletracker.services.SyncTimestampService
import kaist.iclab.mobiletracker.utils.NetworkConditionChecker
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

/**
 * Decides WHEN sensor data should sync to Supabase, based on the configured interval and network
 * preference — replaces the old `AutoSyncService`'s in-process `while(isActive) { delay(60s) }`
 * loop, which ran inside a plain (non-foreground) background `Service` with no OS-level guarantee
 * of ever running again once killed (Doze / App Standby / low memory, with no AlarmManager or
 * WorkManager backstop). This worker is scheduled through WorkManager instead, which — via its
 * JobScheduler/AlarmManager integration — keeps re-triggering the chain across Doze maintenance
 * windows and process death, rather than silently going quiet until the app is reopened.
 *
 * Self-reschedules a `OneTimeWorkRequest` for the next run rather than using a
 * `PeriodicWorkRequest`, since the shortest configurable interval (5 min, see
 * [Constants.AutoSync]) is under WorkManager's 15-minute `PeriodicWorkRequest` floor. Each run
 * re-reads the current interval, so a settings change takes effect on the next tick without
 * needing to cancel/re-enqueue anything.
 *
 * The actual upload work is delegated to [DataUploadService] (a real foreground service, shared
 * with the manual "Upload Now" action) — this worker only decides whether to trigger it, and does
 * NOT await its completion: [DataUploadService] runs to completion on its own regardless of this
 * worker's lifecycle, so awaiting here would only risk this worker being killed for overrunning
 * WorkManager's execution time budget on a large upload.
 */
class SensorAutoSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    companion object {
        private const val TAG = "SensorAutoSyncWorker"

        /**
         * (Re)schedules the next run. Called both to kick off the chain (from [kaist.iclab.mobiletracker.services.AutoSyncManager.start])
         * and by the worker itself at the end of every run.
         */
        fun scheduleNext(context: Context, delayMs: Long) {
            val request = OneTimeWorkRequestBuilder<SensorAutoSyncWorker>()
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                Constants.AutoSync.WORK_NAME_SENSOR_SYNC,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(Constants.AutoSync.WORK_NAME_SENSOR_SYNC)
        }
    }

    private val syncTimestampService: SyncTimestampService by inject()

    override suspend fun doWork(): Result {
        val intervalMs = syncTimestampService.getAutoSyncIntervalMs()
        if (intervalMs == Constants.AutoSync.INTERVAL_NONE) {
            // Auto-sync turned off — don't reschedule; AutoSyncManager.start() (re-enabling it)
            // is what restarts the chain.
            Log.d(TAG, "Auto-sync interval is NONE, stopping chain")
            return Result.success()
        }

        try {
            if (syncTimestampService.getDataCollectionStarted() != null) {
                val networkMode = syncTimestampService.getAutoSyncNetworkMode()
                if (NetworkConditionChecker.isMet(applicationContext, networkMode)) {
                    // Fire-and-forget: DataUploadService is a foreground service that completes
                    // this cycle on its own; see class doc for why we don't await it here.
                    DataUploadService.start(applicationContext)
                } else {
                    Log.w(TAG, "Network condition not met (mode=$networkMode), skipping this cycle")
                }
            } else {
                Log.d(TAG, "Data collection not started, skipping this cycle")
            }
        } catch (e: Exception) {
            // Never let a single bad cycle break the chain — always fall through to reschedule.
            Log.e(TAG, "Error in auto-sync cycle: ${e.message}", e)
        }

        scheduleNext(applicationContext, intervalMs)
        return Result.success()
    }
}
