package kaist.iclab.mobiletracker.services

import android.content.Context
import kaist.iclab.mobiletracker.Constants
import kaist.iclab.mobiletracker.services.upload.SensorAutoSyncWorker
import kaist.iclab.mobiletracker.services.upload.WebAppLogSyncWorker

/**
 * Thin, injectable wrapper around starting/stopping the WorkManager-scheduled auto-sync chain
 * ([SensorAutoSyncWorker] + [WebAppLogSyncWorker]) — replaces the old `AutoSyncService`
 * start/stop lifecycle. See [SensorAutoSyncWorker]'s doc comment for why this moved off a plain
 * background `Service` with an in-process loop.
 *
 * Holds the application [Context] (injected via DI) so callers such as ViewModels can start and
 * stop auto-sync without keeping a Context reference of their own.
 */
class AutoSyncManager(
    private val context: Context,
    private val syncTimestampService: SyncTimestampService
) {
    fun start() {
        // First run happens one full interval from now, matching the old AutoSyncService's
        // behavior (it set lastSyncTime = now on start, so its first *actual* check-and-sync
        // wasn't due until a full interval had elapsed).
        val intervalMs = syncTimestampService.getAutoSyncIntervalMs()
        if (intervalMs != Constants.AutoSync.INTERVAL_NONE) {
            SensorAutoSyncWorker.scheduleNext(context, delayMs = intervalMs)
        }
        // WebApp log flushing runs immediately and independent of the sensor-sync interval.
        WebAppLogSyncWorker.scheduleNext(context, delayMs = 0L)
    }

    fun stop() {
        SensorAutoSyncWorker.cancel(context)
        WebAppLogSyncWorker.cancel(context)
    }
}
