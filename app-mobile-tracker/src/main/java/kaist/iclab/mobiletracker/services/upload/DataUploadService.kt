package kaist.iclab.mobiletracker.services.upload

import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kaist.iclab.mobiletracker.Constants
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.db.obx.MicroEmaResponseStore
import kaist.iclab.mobiletracker.helpers.LanguageHelper
import kaist.iclab.mobiletracker.repository.DataRepository
import kaist.iclab.mobiletracker.repository.Result
import kaist.iclab.mobiletracker.repository.handlers.SurveyResponseDataHandler
import kaist.iclab.mobiletracker.repository.handlers.WebAppLogDataHandler
import kaist.iclab.mobiletracker.services.SurveyService
import kaist.iclab.mobiletracker.services.upload.DataUploadService.Companion.completionEvents
import kaist.iclab.mobiletracker.services.upload.DataUploadService.Companion.uploadState
import kaist.iclab.mobiletracker.ui.utils.getSensorTitleResId
import kaist.iclab.mobiletracker.utils.NotificationHelper
import kaist.iclab.mobiletracker.utils.SensorTypeHelper
import kaist.iclab.mobiletracker.utils.SupabaseLoadingInterceptor
import kaist.iclab.tracker.sensor.core.Sensor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent
import org.koin.core.qualifier.named
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger

/**
 * Current state of an in-flight upload, shared across triggers (manual "Upload Now" and
 * auto-sync) and observed by the UI to render a non-blocking progress indicator.
 */
sealed interface DataUploadState {
    data object Idle : DataUploadState
    data class InProgress(
        val currentSensorName: String,
        val currentBatch: Int,
        val totalBatches: Int
    ) : DataUploadState
}

/**
 * Per-sensor upload outcome for one run, in records — how much of what was attempted actually
 * landed, so the summary UI can show "1,230 / 1,250 (98%)" instead of a bare pass/fail flag.
 */
data class SensorUploadResult(
    val displayName: String,
    val succeededCount: Int,
    val attemptedCount: Int,
    val isError: Boolean
) {
    /** `null` when nothing was attempted (e.g. [isError] before any batch landed). */
    val successRatePercent: Int?
        get() = if (attemptedCount == 0) null else (succeededCount * 100) / attemptedCount
}

/** One-shot summary emitted when an upload run finishes, regardless of trigger. */
data class UploadSummary(
    /** Sensors that had something to report — anything attempted, or an error before anything did. */
    val results: List<SensorUploadResult>,
    val upToDateCount: Int,
    val upToDateSensors: List<String>
)

/**
 * Foreground service that performs all data uploads to Supabase — both the manual "Upload Now"
 * action ([DataRepository]-driven, per selected sensor) and scheduled auto-sync cycles (every
 * active sensor + survey + MicroEMA responses) are routed through here.
 *
 * Running the upload as a foreground service — instead of in-process on the caller, as before —
 * means neither trigger has to block the UI or fight over [SupabaseLoadingInterceptor]'s global
 * loading overlay: this service owns suppressing that overlay for its entire run, and instead
 * surfaces progress via [uploadState] / [completionEvents] (consumed by the UI for an in-app,
 * non-blocking progress bar) and an ongoing notification with a real progress bar. A single
 * result notification (success/failure counts) is shown when the run finishes.
 */
class DataUploadService : LifecycleService(), KoinComponent {
    companion object {
        private const val TAG = "DataUploadService"
        private const val EXTRA_SENSOR_IDS = "extra_sensor_ids"

        private val _uploadState = MutableStateFlow<DataUploadState>(DataUploadState.Idle)
        val uploadState: StateFlow<DataUploadState> = _uploadState.asStateFlow()

        private val _completionEvents = MutableSharedFlow<UploadSummary>(extraBufferCapacity = 1)
        val completionEvents: SharedFlow<UploadSummary> = _completionEvents.asSharedFlow()

        private val lock = Any()

        /**
         * Every caller waiting on the current run, not just the most recent one. A single
         * nullable Deferred was not enough: [notifyCompletion] cleared it while the run's
         * coroutine was still inside its `finally`, so a [start] landing in that window installed
         * a fresh Deferred that the (already finished) run would never complete and that the next
         * [onStartCommand] would dismiss as "a run is already underway". The caller —
         * AutoSyncService, which awaits this — then hung forever with its `isSyncing` flag stuck
         * true, permanently disabling auto-sync for the rest of the process's life.
         *
         * Parking a Deferred here and claiming/releasing a run both happen under [lock], so a
         * request either joins the in-flight run or starts a new one — it can never fall between
         * the two.
         */
        private val pendingCompletions = mutableListOf<CompletableDeferred<Unit>>()

        /** Whether a run has been claimed by [onStartCommand] and not yet released. */
        private var runInFlight = false

        /**
         * Starts the upload foreground service and returns a [Deferred] that completes once the
         * triggered run finishes. If a run is already in progress, no duplicate run is started —
         * the returned Deferred simply completes alongside the run already in flight.
         *
         * `startForegroundService()` itself can throw — most notably
         * `ForegroundServiceStartNotAllowedException` (API 31+) when the app has no qualifying
         * foreground/visible state. AutoSyncService being itself a foreground service makes this
         * unlikely in practice, but the manual "Upload Now" path calls this too and isn't
         * guaranteed to always run from a visible Activity either, so it's caught here rather
         * than left to propagate: an uncaught exception here — called from an unstructured
         * `lifecycleScope.launch` in AutoSyncService, with no surrounding catch — would crash the
         * whole process rather than just skip this sync cycle. On failure the returned Deferred
         * still completes normally rather than exceptionally, so a caller that awaits it (like
         * AutoSyncService's `checkAndSyncIfNeeded`) doesn't hang or crash; the cycle is simply
         * treated as a no-op and retried next time.
         *
         * @param sensorIds Specific sensors to upload, e.g. from the "Upload Now" button. Pass
         * null for a full sync of every active sensor plus survey/MicroEMA responses, used by
         * auto-sync.
         */
        fun start(context: Context, sensorIds: List<String>? = null): Deferred<Unit> {
            val deferred = CompletableDeferred<Unit>()
            val joinsRunInFlight = synchronized(lock) {
                pendingCompletions.add(deferred)
                runInFlight
            }
            // Ride along with the run already underway rather than starting a duplicate — and
            // without re-posting the foreground notification, which would reset the visible
            // progress bar. That run's notifyCompletion() completes this Deferred too.
            if (joinsRunInFlight) return deferred

            try {
                val intent = Intent(context, DataUploadService::class.java).apply {
                    sensorIds?.let { putStringArrayListExtra(EXTRA_SENSOR_IDS, ArrayList(it)) }
                }
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start DataUploadService, will retry next cycle: ${e.message}", e)
                synchronized(lock) { pendingCompletions.remove(deferred) }
                deferred.complete(Unit)
            }
            return deferred
        }

        /** Claims the run for the calling [onStartCommand], or reports one already in flight. */
        private fun claimRun(): Boolean = synchronized(lock) {
            if (runInFlight) {
                false
            } else {
                runInFlight = true
                true
            }
        }

        /**
         * Releases the run and completes everyone waiting on it. Idempotent — completing an
         * already-completed Deferred is a no-op — so the safety-net call in [onDestroy] can't
         * double-complete or strand anything.
         */
        private fun notifyCompletion() {
            val waiting = synchronized(lock) {
                runInFlight = false
                val snapshot = pendingCompletions.toList()
                pendingCompletions.clear()
                snapshot
            }
            waiting.forEach { it.complete(Unit) }
        }
    }

    private val dataRepository: DataRepository by inject()
    private val sensorUploadService: SensorUploadService by inject()
    private val sensors by inject<List<Sensor<*, *>>>(qualifier = named("phoneSensors"))
    private val surveyService: SurveyService by inject()
    private val microEmaResponseDao by inject<MicroEmaResponseStore>()
    private val surveyResponseUploader: SurveyResponseUploader by inject()

    /**
     * The most recent startId the system has delivered, including requests that only rode along
     * with an in-flight run. `stopSelf(startId)` is a no-op unless the id is the latest one
     * delivered, so stopping with the id captured when the run began would leave the service
     * alive — and no longer in the foreground — whenever a second request arrived mid-run.
     */
    @Volatile
    private var latestStartId: Int = 0

    /**
     * The coroutine doing the actual uploading, kept so [onTimeout] can cut it short: the system
     * gives only a few seconds there before it kills the app outright.
     */
    @Volatile
    private var uploadJob: Job? = null

    /**
     * Resolved once instead of per notification update: progress is now posted per upload batch,
     * which is far more often than the old per-sensor updates.
     */
    private val localizedContext by lazy { LanguageHelper(this).applyLanguage(this) }

    /**
     * Localized sensor name for progress/notification/summary text — the same
     * [getSensorTitleResId] mapping the Data tab uses via its Composable [getSensorDisplayName]
     * wrapper, just resolved through [localizedContext] since this runs outside Compose. Handler
     * `displayName` (used before) is a fixed English label, not localized.
     */
    private fun localizedDisplayName(sensorId: String): String =
        localizedContext.getString(getSensorTitleResId(sensorId))

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        latestStartId = startId

        NotificationHelper.ensureNotificationChannel(
            this,
            Constants.Notification.CHANNEL_ID_DATA_UPLOAD,
            Constants.Notification.CHANNEL_NAME_DATA_UPLOAD
        )
        NotificationHelper.ensureNotificationChannel(
            this,
            Constants.Notification.CHANNEL_ID_DATA_UPLOAD_PROGRESS,
            Constants.Notification.CHANNEL_NAME_DATA_UPLOAD_PROGRESS,
            android.app.NotificationManager.IMPORTANCE_LOW
        )
        try {
            startForegroundWithNotification(buildProgressNotification("", 0, 0))
        } catch (e: Exception) {
            // The system can refuse foreground status outright — ForegroundServiceStartNotAllowedException
            // for a background start on API 31+, or once a timed-out service type has spent its quota
            // (see [onTimeout]). start() guards its own startForegroundService() call, but this one
            // runs inside the service, where an escaping exception takes down the whole process
            // rather than just skipping a cycle.
            Log.e(TAG, "Cannot enter the foreground; skipping this upload: ${e.message}", e)
            // Only tear down if no run is already underway. If one is, it owns the foreground
            // notification and will complete this request's Deferred through notifyCompletion() as
            // usual — stopping the service here would kill an upload that is working fine.
            if (claimRun()) {
                notifyCompletion()
                stopSelf(startId)
            }
            return START_NOT_STICKY
        }

        if (!claimRun()) {
            // A run is already underway; this request rides along with it — start() has already
            // parked its Deferred for the in-flight run's notifyCompletion() to complete.
            return START_NOT_STICKY
        }

        val requestedSensorIds = intent?.getStringArrayListExtra(EXTRA_SENSOR_IDS)
        val tally = UploadTally()
        uploadJob = lifecycleScope.launch(Dispatchers.IO) {
            SupabaseLoadingInterceptor.suppressGlobalLoading = true
            try {
                if (requestedSensorIds != null) {
                    runSelectedUpload(requestedSensorIds, tally)
                } else {
                    runFullSync(tally)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "Fatal error during upload: ${e.message}", e)
            } finally {
                // The summary is emitted here rather than at the end of the run functions so that
                // a run which ends early — a child job failing through awaitAll(), or the service
                // being torn down mid-upload — still reports whatever it managed to upload. It is
                // also the UI's only "the run is over" signal: DataViewModel keeps rendering the
                // last InProgress snapshot until something replaces it, so skipping this on the
                // error path left the progress indicator spinning and the Upload button disabled
                // for good. NonCancellable because emit() throws immediately on a cancelled job.
                withContext(NonCancellable) { finishAndNotify(tally.toSummary()) }
                SupabaseLoadingInterceptor.suppressGlobalLoading = false
                _uploadState.value = DataUploadState.Idle
                notifyCompletion()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf(latestStartId)
            }
        }
        return START_NOT_STICKY
    }

    /**
     * Released here as well as in the run's `finally` so that a teardown which never reaches that
     * `finally` can't leave [runInFlight] stuck true — which would wedge every future upload,
     * manual and automatic alike.
     */
    override fun onDestroy() {
        notifyCompletion()
        super.onDestroy()
    }

    /**
     * Nothing should reach this while the service runs as `specialUse`, which carries no runtime
     * cap — it is here because the type this service used to have, `dataSync`, does: API 34+ allows
     * one only ~6 hours per 24 hours, counted cumulatively across runs, and kills an app that
     * doesn't stop at the limit with `ForegroundServiceDidNotStopInTimeException`. That crash is
     * what large uploads were hitting. Kept as a safety net in case the type moves back, and for
     * Google extending timeouts to further types the way API 35 did to `mediaProcessing`.
     *
     * Stopping here is safe and nearly free: every batch that landed has already persisted its
     * cursor, and the run's own `finally` still emits a summary of what got through, so the next
     * cycle resumes from where this left off.
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onTimeout(startId: Int) {
        handleForegroundTimeout(startId)
    }

    /** API 35+ calls this overload instead of [onTimeout]; both must be handled. */
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onTimeout(startId: Int, fgsType: Int) {
        handleForegroundTimeout(startId)
    }

    private fun handleForegroundTimeout(startId: Int) {
        Log.w(TAG, "dataSync foreground service timed out; pausing the upload (startId=$startId)")
        // Cancelling rather than waiting for the run to notice: the deadline here is a few seconds,
        // far less than a batch can take. The run's finally still reports what it managed to send.
        uploadJob?.cancel(CancellationException("dataSync foreground service timed out"))
        SupabaseLoadingInterceptor.suppressGlobalLoading = false
        _uploadState.value = DataUploadState.Idle
        notifyCompletion()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startForegroundWithNotification(notification: android.app.Notification) {
        // Must match android:foregroundServiceType in the manifest — startForeground() rejects a
        // type that wasn't declared there. See the manifest for why this is specialUse, not dataSync.
        val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        startForeground(Constants.Notification.ID_DATA_UPLOAD_PROGRESS, notification, serviceType)
    }

    /**
     * Progress accounting for a run, measured in upload batches rather than sensors: a sensor
     * sitting on a 50k-row backlog is ~100 batches of work while an idle one is a single unit, so
     * a per-sensor bar crawled through the big ones and then leapt through the rest. The total is
     * estimated up front from each sensor's pending row count.
     */
    private class BatchProgress(estimatedTotal: Int) {
        private val total = AtomicInteger(estimatedTotal)
        private val completed = AtomicInteger(0)

        val totalBatches: Int get() = total.get()
        val completedBatches: Int get() = completed.get()

        /**
         * Records one finished batch, growing the total if the run outlasts the estimate — sensors
         * keep collecting while the upload runs, so the backlog can outgrow its own measurement.
         */
        fun advance() {
            val done = completed.incrementAndGet()
            total.updateAndGet { maxOf(it, done) }
        }
    }

    /**
     * Running tally of an upload run. Owned by [onStartCommand] rather than by the run functions
     * so the `finally` there can still emit a summary — of whatever completed — when the run ends
     * early. The lists are synchronized because [runFullSync] fills them from parallel jobs.
     */
    private class UploadTally {
        val results: MutableList<SensorUploadResult> = Collections.synchronizedList(mutableListOf())
        val upToDate: MutableList<String> = Collections.synchronizedList(mutableListOf())

        /** Snapshot of the tally so far; safe to take while parallel jobs are still adding. */
        fun toSummary(): UploadSummary {
            val resultsSnapshot = synchronized(results) { results.toList() }
            val upToDateSnapshot = synchronized(upToDate) { upToDate.toList() }
            return UploadSummary(
                results = resultsSnapshot,
                upToDateCount = upToDateSnapshot.size,
                upToDateSensors = upToDateSnapshot
            )
        }
    }

    /** Uploads exactly the requested sensors, sequentially — mirrors the old "Upload Now" logic. */
    private suspend fun runSelectedUpload(sensorIds: List<String>, tally: UploadTally) {
        val results = tally.results
        val upToDateSensors = tally.upToDate
        val progress = BatchProgress(sensorIds.sumOf { estimatedBatchesFor(it) })

        sensorIds.forEach { sensorId ->
            val displayName = localizedDisplayName(sensorId)
            // Announced before the work starts, so the name on screen is the sensor currently
            // uploading rather than the one that just finished.
            updateProgress(displayName, progress)

            // Survey/WebApp log aren't gated by campaign membership (see SensorUploadService),
            // so only skip the campaign check for sensors that actually go through it. A sensor
            // dropped from the campaign has nothing to upload for the same reason an up-to-date
            // one doesn't, but reporting it as "already up to date" would wrongly imply it's still
            // being tracked — so it's left out of the summary entirely instead.
            if (isCampaignGatedSensor(sensorId) && !sensorUploadService.isSensorActive(sensorId)) {
                Log.d(TAG, "Skipping $sensorId: not part of the current campaign")
                return@forEach
            }

            try {
                val outcome = dataRepository.uploadSensorData(sensorId) {
                    progress.advance()
                    updateProgress(displayName, progress)
                }
                if (outcome.isUpToDate) {
                    upToDateSensors.add(displayName)
                } else {
                    results.add(SensorUploadResult(displayName, outcome.succeededCount, outcome.attemptedCount, outcome.isError))
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                results.add(SensorUploadResult(displayName, 0, 0, isError = true))
                Log.e(TAG, "Error uploading $sensorId", e)
            }
        }
    }

    /** True for sensors whose upload is gated on campaign membership (i.e. not Survey/WebAppLog). */
    private fun isCampaignGatedSensor(sensorId: String): Boolean =
        sensorId != SurveyResponseDataHandler.SENSOR_ID && sensorId != WebAppLogDataHandler.SENSOR_ID

    /**
     * Batches [sensorId] is expected to need. Survey and webapp logs drain in a single flush
     * instead of cursor-tracked batches, so they count as one unit each.
     */
    private suspend fun estimatedBatchesFor(sensorId: String): Int =
        if (isCampaignGatedSensor(sensorId)) sensorUploadService.pendingBatchCount(sensorId) else 1

    /**
     * Uploads every active sensor plus survey/MicroEMA responses in parallel — same shape as the
     * old AutoSyncService.uploadAllSensorData(), just relocated here so auto-sync and manual
     * upload share one implementation and one progress/notification surface.
     */
    private suspend fun runFullSync(tally: UploadTally) {
        // supervisorScope, so the jobs below are children of this call rather than of
        // lifecycleScope. Cancelling the run has to actually stop the uploading: [onTimeout]
        // cancels uploadJob, and jobs launched on lifecycleScope are that job's siblings, not its
        // children — they would carry on uploading while the service tears down around them.
        // Supervisor rather than plain coroutineScope to keep the existing behaviour that one
        // sensor blowing up doesn't cancel the others.
        supervisorScope {
            // Only sensors currently included in the joined campaign are uploaded — a sensor that
            // was removed from (or never added to) the campaign has nothing to upload for the same
            // reason an up-to-date one doesn't, but listing it as "already up to date" in the summary
            // would wrongly imply it's still being tracked, so it's excluded outright instead.
            val allSensorIds = (sensors.map { it.id } + SensorTypeHelper.watchSensorIds)
                .filter { sensorUploadService.isSensorActive(it) }
            val results = tally.results
            val upToDateSensors = tally.upToDate
            // + 1 unit each for MicroEMA and Survey, which flush in one go rather than in batches.
            val progress = BatchProgress(allSensorIds.sumOf { sensorUploadService.pendingBatchCount(it) } + 2)

            updateProgress("", progress)

            // Sensors upload concurrently, so the name shown is whichever of them most recently
            // started or finished a batch — always one that is genuinely in flight.
            val sensorJobs = allSensorIds.map { sensorId ->
                async(Dispatchers.IO) {
                    val displayName = localizedDisplayName(sensorId)
                    if (sensorUploadService.hasDataToUpload(sensorId)) {
                        updateProgress(displayName, progress)
                        val outcome = sensorUploadService.uploadSensorData(sensorId) {
                            progress.advance()
                            updateProgress(displayName, progress)
                        }
                        if (outcome.isError) {
                            Log.e(TAG, "Upload failed for $sensorId: ${outcome.error?.message}", outcome.error)
                        }
                        results.add(SensorUploadResult(displayName, outcome.succeededCount, outcome.attemptedCount, outcome.isError))
                    } else {
                        upToDateSensors.add(displayName)
                    }
                }
            }

            val microEmaJob = async(Dispatchers.IO) {
                // No quarantine concept for MicroEMA/Survey — they're insert-only flushes, so
                // result.data (the count Supabase actually accepted) doubles as both succeeded and
                // attempted.
                when (val result = surveyService.uploadUnsyncedMicroEmaResponses(microEmaResponseDao)) {
                    is Result.Success -> {
                        if (result.data > 0) results.add(SensorUploadResult("MicroEMA", result.data, result.data, isError = false))
                        else upToDateSensors.add("MicroEMA")
                    }

                    is Result.Error -> {
                        Log.e(TAG, "Failed to upload MicroEMA responses: ${result.message}", result.exception)
                        results.add(SensorUploadResult("MicroEMA", 0, 0, isError = true))
                    }
                }
                progress.advance()
                updateProgress("MicroEMA", progress)
            }

            val surveyJob = async(Dispatchers.IO) {
                val displayName = localizedDisplayName(SurveyResponseDataHandler.SENSOR_ID)
                when (val result = surveyResponseUploader.flush()) {
                    is Result.Success -> {
                        if (result.data > 0) results.add(SensorUploadResult(displayName, result.data, result.data, isError = false))
                        else upToDateSensors.add(displayName)
                    }

                    is Result.Error -> {
                        Log.e(TAG, "Failed to upload survey responses: ${result.message}", result.exception)
                        results.add(SensorUploadResult(displayName, 0, 0, isError = true))
                    }
                }
                progress.advance()
                updateProgress(displayName, progress)
            }

            (sensorJobs + microEmaJob + surveyJob).awaitAll()
        }

    }

    private fun updateProgress(currentLabel: String, progress: BatchProgress) {
        val current = progress.completedBatches
        val total = progress.totalBatches
        _uploadState.value = DataUploadState.InProgress(
            currentSensorName = currentLabel,
            currentBatch = current,
            totalBatches = total
        )
        NotificationHelper.showNotification(
            this,
            Constants.Notification.ID_DATA_UPLOAD_PROGRESS,
            buildProgressNotification(currentLabel, current, total)
        )
    }

    private fun buildProgressNotification(
        currentLabel: String,
        current: Int,
        total: Int
    ): android.app.Notification {
        val text = when {
            total <= 0 -> localizedContext.getString(R.string.sync_status_uploading)
            currentLabel.isBlank() ->
                localizedContext.getString(R.string.upload_dialog_progress, current, total)

            else -> localizedContext.getString(
                R.string.upload_dialog_progress_sensor,
                currentLabel,
                current,
                total
            )
        }
        return NotificationHelper.buildNotification(
            context = this,
            channelId = Constants.Notification.CHANNEL_ID_DATA_UPLOAD_PROGRESS,
            title = localizedContext.getString(R.string.upload_dialog_title),
            text = text,
            priority = NotificationCompat.PRIORITY_LOW,
            ongoing = true,
            autoCancel = false
        ).apply {
            setProgress(total.coerceAtLeast(1), current, total == 0)
            // This notification is re-posted on every batch (see [updateProgress]). Without these
            // two, each re-post alerts again and the phone buzzes for the whole upload: the channel
            // handles API 26+, setOnlyAlertOnce/setSilent cover the pre-O priority path and keep a
            // user who re-enables the channel's importance from getting the ringing back.
            setOnlyAlertOnce(true)
            setSilent(true)
        }.build()
    }

    private suspend fun finishAndNotify(summary: UploadSummary) {
        _completionEvents.emit(summary)

        // Only surface a result notification when there was something to report — matches the
        // previous auto-sync behavior of staying silent on a no-op cycle (nothing new to upload).
        if (summary.results.isNotEmpty()) {
            showResultNotification(summary)
        }
    }

    private fun showResultNotification(summary: UploadSummary) {
        val pendingIntent = NotificationHelper.createMainActivityPendingIntent(
            this,
            Constants.Notification.ID_DATA_UPLOAD_RESULT
        )

        val failed = summary.results.filter { it.isError }
        val succeeded = summary.results.filter { !it.isError }

        val title: String
        val text: String
        if (failed.isNotEmpty()) {
            val failedText = failed.take(3).joinToString(", ") { it.displayName } +
                    if (failed.size > 3) "…" else ""
            title = localizedContext.getString(R.string.data_upload_result_failure_title)
            text = localizedContext.getString(
                R.string.data_upload_result_message,
                succeeded.size,
                failed.size,
                failedText
            )
        } else {
            title = localizedContext.getString(R.string.data_upload_result_success_title)
            text = localizedContext.getString(
                R.string.data_upload_result_success_message,
                succeeded.size
            )
        }

        val notification = NotificationHelper.buildNotification(
            context = this,
            channelId = Constants.Notification.CHANNEL_ID_DATA_UPLOAD,
            title = title,
            text = text,
            pendingIntent = pendingIntent
        ).build()

        NotificationHelper.showNotification(this, Constants.Notification.ID_DATA_UPLOAD_RESULT, notification)
        Log.d(TAG, "Result notification shown: ${succeeded.size} succeeded, ${failed.size} failed")
    }
}
