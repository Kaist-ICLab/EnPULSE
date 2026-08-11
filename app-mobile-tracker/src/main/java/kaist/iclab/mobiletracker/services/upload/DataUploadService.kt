package kaist.iclab.mobiletracker.services.upload

import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
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
import kaist.iclab.mobiletracker.repository.onFailure
import kaist.iclab.mobiletracker.repository.onSuccess
import kaist.iclab.mobiletracker.services.SurveyService
import kaist.iclab.mobiletracker.utils.NotificationHelper
import kaist.iclab.mobiletracker.utils.SensorTypeHelper
import kaist.iclab.mobiletracker.utils.SupabaseLoadingInterceptor
import kaist.iclab.tracker.sensor.core.Sensor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
        val currentIndex: Int,
        val totalSensors: Int
    ) : DataUploadState
}

/** One-shot summary emitted when an upload run finishes, regardless of trigger. */
data class UploadSummary(
    val successCount: Int,
    val failedCount: Int,
    val upToDateCount: Int,
    val successfulSensors: List<String>,
    val failedSensors: List<String>,
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
        private var completionDeferred: CompletableDeferred<Unit>? = null

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
        fun start(context: Context, sensorIds: List<String>? = null): Deferred<Unit> =
            synchronized(lock) {
                val existing = completionDeferred
                if (existing != null && !existing.isCompleted) {
                    return existing
                }
                val deferred = CompletableDeferred<Unit>()
                completionDeferred = deferred
                try {
                    val intent = Intent(context, DataUploadService::class.java).apply {
                        sensorIds?.let { putStringArrayListExtra(EXTRA_SENSOR_IDS, ArrayList(it)) }
                    }
                    ContextCompat.startForegroundService(context, intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start DataUploadService, will retry next cycle: ${e.message}", e)
                    completionDeferred = null
                    deferred.complete(Unit)
                }
                deferred
            }

        private fun notifyCompletion() = synchronized(lock) {
            completionDeferred?.complete(Unit)
            completionDeferred = null
        }
    }

    private val dataRepository: DataRepository by inject()
    private val sensorUploadService: SensorUploadService by inject()
    private val sensors by inject<List<Sensor<*, *>>>(qualifier = named("phoneSensors"))
    private val surveyService: SurveyService by inject()
    private val microEmaResponseDao by inject<MicroEmaResponseStore>()
    private val surveyResponseUploader: SurveyResponseUploader by inject()

    /** Guards against starting a second run while one is already in flight; see [onStartCommand]. */
    private var activeJob: Job? = null

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        NotificationHelper.ensureNotificationChannel(
            this,
            Constants.Notification.CHANNEL_ID_DATA_UPLOAD,
            Constants.Notification.CHANNEL_NAME_DATA_UPLOAD
        )
        startForegroundWithNotification(buildProgressNotification(0, 0))

        if (activeJob?.isActive == true) {
            // A run is already underway; the new request rides along with it via the shared
            // completionDeferred set up in start().
            return START_NOT_STICKY
        }

        val requestedSensorIds = intent?.getStringArrayListExtra(EXTRA_SENSOR_IDS)
        activeJob = lifecycleScope.launch(Dispatchers.IO) {
            SupabaseLoadingInterceptor.suppressGlobalLoading = true
            try {
                if (requestedSensorIds != null) {
                    runSelectedUpload(requestedSensorIds)
                } else {
                    runFullSync()
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "Fatal error during upload: ${e.message}", e)
            } finally {
                SupabaseLoadingInterceptor.suppressGlobalLoading = false
                _uploadState.value = DataUploadState.Idle
                notifyCompletion()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf(startId)
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundWithNotification(notification: android.app.Notification) {
        val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else {
            0
        }
        startForeground(Constants.Notification.ID_DATA_UPLOAD_PROGRESS, notification, serviceType)
    }

    /** Uploads exactly the requested sensors, sequentially — mirrors the old "Upload Now" logic. */
    private suspend fun runSelectedUpload(sensorIds: List<String>) {
        val total = sensorIds.size
        val successfulSensors = mutableListOf<String>()
        val failedSensors = mutableListOf<String>()
        val upToDateSensors = mutableListOf<String>()

        sensorIds.forEachIndexed { index, sensorId ->
            val displayName = dataRepository.getSensorInfo(sensorId)?.displayName ?: sensorId
            updateProgress(displayName, index + 1, total)

            // Survey/WebApp log aren't gated by campaign membership (see SensorUploadService),
            // so only skip the campaign check for sensors that actually go through it. A sensor
            // dropped from the campaign has nothing to upload for the same reason an up-to-date
            // one doesn't, but reporting it as "already up to date" would wrongly imply it's still
            // being tracked — so it's left out of the summary entirely instead.
            if (isCampaignGatedSensor(sensorId) && !sensorUploadService.isSensorActive(sensorId)) {
                Log.d(TAG, "Skipping $sensorId: not part of the current campaign")
                return@forEachIndexed
            }

            try {
                val result = dataRepository.uploadSensorData(sensorId)
                when {
                    result > 0 -> successfulSensors.add(displayName)
                    result == -1 -> failedSensors.add(displayName)
                    else -> upToDateSensors.add(displayName)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                failedSensors.add(displayName)
                Log.e(TAG, "Error uploading $sensorId", e)
            }
        }

        finishAndNotify(successfulSensors, failedSensors, upToDateSensors)
    }

    /** True for sensors whose upload is gated on campaign membership (i.e. not Survey/WebAppLog). */
    private fun isCampaignGatedSensor(sensorId: String): Boolean =
        sensorId != SurveyResponseDataHandler.SENSOR_ID && sensorId != WebAppLogDataHandler.SENSOR_ID

    /**
     * Uploads every active sensor plus survey/MicroEMA responses in parallel — same shape as the
     * old AutoSyncService.uploadAllSensorData(), just relocated here so auto-sync and manual
     * upload share one implementation and one progress/notification surface.
     */
    private suspend fun runFullSync() {
        // Only sensors currently included in the joined campaign are uploaded — a sensor that
        // was removed from (or never added to) the campaign has nothing to upload for the same
        // reason an up-to-date one doesn't, but listing it as "already up to date" in the summary
        // would wrongly imply it's still being tracked, so it's excluded outright instead.
        val allSensorIds = (sensors.map { it.id } + SensorTypeHelper.watchSensorIds)
            .filter { sensorUploadService.isSensorActive(it) }
        val totalUnits = allSensorIds.size + 2 // + MicroEMA + Survey
        val completed = AtomicInteger(0)
        val successfulSensors = Collections.synchronizedList(mutableListOf<String>())
        val failedSensors = Collections.synchronizedList(mutableListOf<String>())
        val upToDateSensors = Collections.synchronizedList(mutableListOf<String>())

        updateProgress("", 0, totalUnits)

        val sensorJobs = allSensorIds.map { sensorId ->
            lifecycleScope.async(Dispatchers.IO) {
                if (sensorUploadService.hasDataToUpload(sensorId)) {
                    sensorUploadService.uploadSensorData(sensorId)
                        .onSuccess { successfulSensors.add(sensorId) }
                        .onFailure { e ->
                            failedSensors.add(sensorId)
                            Log.e(TAG, "Upload failed for $sensorId: ${e.message}", e)
                        }
                } else {
                    upToDateSensors.add(sensorId)
                }
                updateProgress(sensorId, completed.incrementAndGet(), totalUnits)
            }
        }

        val microEmaJob = lifecycleScope.async(Dispatchers.IO) {
            when (val result = surveyService.uploadUnsyncedMicroEmaResponses(microEmaResponseDao)) {
                is Result.Success -> {
                    if (result.data > 0) successfulSensors.add("MicroEMA") else upToDateSensors.add("MicroEMA")
                }

                is Result.Error -> {
                    Log.e(TAG, "Failed to upload MicroEMA responses: ${result.message}", result.exception)
                    failedSensors.add("MicroEMA")
                }
            }
            updateProgress("MicroEMA", completed.incrementAndGet(), totalUnits)
        }

        val surveyJob = lifecycleScope.async(Dispatchers.IO) {
            when (val result = surveyResponseUploader.flush()) {
                is Result.Success -> {
                    if (result.data > 0) successfulSensors.add("Survey") else upToDateSensors.add("Survey")
                }

                is Result.Error -> {
                    Log.e(TAG, "Failed to upload survey responses: ${result.message}", result.exception)
                    failedSensors.add("Survey")
                }
            }
            updateProgress("Survey", completed.incrementAndGet(), totalUnits)
        }

        (sensorJobs + microEmaJob + surveyJob).awaitAll()

        finishAndNotify(successfulSensors.toList(), failedSensors.toList(), upToDateSensors.toList())
    }

    private fun updateProgress(currentLabel: String, index: Int, total: Int) {
        _uploadState.value = DataUploadState.InProgress(
            currentSensorName = currentLabel,
            currentIndex = index,
            totalSensors = total
        )
        NotificationHelper.showNotification(
            this,
            Constants.Notification.ID_DATA_UPLOAD_PROGRESS,
            buildProgressNotification(index, total)
        )
    }

    private fun buildProgressNotification(current: Int, total: Int): android.app.Notification {
        val localizedContext = LanguageHelper(this).applyLanguage(this)
        val text = if (total > 0) {
            localizedContext.getString(R.string.upload_dialog_progress, current, total)
        } else {
            localizedContext.getString(R.string.sync_status_uploading)
        }
        return NotificationHelper.buildNotification(
            context = this,
            channelId = Constants.Notification.CHANNEL_ID_DATA_UPLOAD,
            title = localizedContext.getString(R.string.upload_dialog_title),
            text = text,
            ongoing = true,
            autoCancel = false
        ).apply {
            setProgress(total.coerceAtLeast(1), current, total == 0)
        }.build()
    }

    private suspend fun finishAndNotify(
        successfulSensors: List<String>,
        failedSensors: List<String>,
        upToDateSensors: List<String>
    ) {
        val summary = UploadSummary(
            successCount = successfulSensors.size,
            failedCount = failedSensors.size,
            upToDateCount = upToDateSensors.size,
            successfulSensors = successfulSensors,
            failedSensors = failedSensors,
            upToDateSensors = upToDateSensors
        )
        _completionEvents.emit(summary)

        // Only surface a result notification when there was something to report — matches the
        // previous auto-sync behavior of staying silent on a no-op cycle (nothing new to upload).
        if (summary.successCount > 0 || summary.failedCount > 0) {
            showResultNotification(summary)
        }
    }

    private fun showResultNotification(summary: UploadSummary) {
        val localizedContext = LanguageHelper(this).applyLanguage(this)
        val pendingIntent = NotificationHelper.createMainActivityPendingIntent(
            this,
            Constants.Notification.ID_DATA_UPLOAD_RESULT
        )

        val title: String
        val text: String
        if (summary.failedCount > 0) {
            val failedText = summary.failedSensors.take(3).joinToString(", ") +
                    if (summary.failedSensors.size > 3) "…" else ""
            title = localizedContext.getString(R.string.data_upload_result_failure_title)
            text = localizedContext.getString(
                R.string.data_upload_result_message,
                summary.successCount,
                summary.failedCount,
                failedText
            )
        } else {
            title = localizedContext.getString(R.string.data_upload_result_success_title)
            text = localizedContext.getString(
                R.string.data_upload_result_success_message,
                summary.successCount
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
        Log.d(TAG, "Result notification shown: ${summary.successCount} succeeded, ${summary.failedCount} failed")
    }
}
