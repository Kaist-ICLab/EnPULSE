package kaist.iclab.mobiletracker.services

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kaist.iclab.mobiletracker.Constants
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.helpers.LanguageHelper
import kaist.iclab.mobiletracker.repository.onFailure
import kaist.iclab.mobiletracker.repository.onSuccess
import kaist.iclab.mobiletracker.services.upload.PhoneSensorUploadService
import kaist.iclab.mobiletracker.services.upload.WatchSensorUploadService
import kaist.iclab.mobiletracker.utils.NotificationHelper
import kaist.iclab.mobiletracker.utils.SensorTypeHelper
import kaist.iclab.tracker.sensor.core.Sensor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent
import org.koin.core.qualifier.named
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Service that handles automatic synchronization of sensor data to Supabase
 * based on configured interval and network preferences.
 *
 * Uses LifecycleService for automatic coroutine lifecycle management.
 * Sensor uploads are parallelized and protected by a sync lock to prevent
 * overlapping sync cycles.
 */
class AutoSyncService : LifecycleService(), KoinComponent {
    companion object {
        private const val TAG = "AutoSyncService"

        /**
         * Helper function to start the service from a Context
         */
        fun start(context: Context) {
            val intent = Intent(context, AutoSyncService::class.java)
            context.startService(intent)
        }

        /**
         * Helper function to stop the service from a Context
         */
        fun stop(context: Context) {
            val intent = Intent(context, AutoSyncService::class.java)
            context.stopService(intent)
        }
    }

    private val syncTimestampService by inject<SyncTimestampService>()
    private val phoneSensorUploadService: PhoneSensorUploadService by inject()
    private val watchSensorUploadService: WatchSensorUploadService by inject()
    private val sensors by inject<List<Sensor<*, *>>>(qualifier = named("phoneSensors"))

    private var lastSyncTime: Long = 0

    /** Guards against duplicate sync loops from repeated onStartCommand calls */
    private var syncLoopJob: Job? = null

    /** Prevents overlapping sync cycles when uploads take longer than the interval */
    private val isSyncing = AtomicBoolean(false)

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startAutoSync()
        return START_STICKY
    }

    /**
     * Starts the auto-sync coroutine that periodically checks and syncs data.
     * Uses lifecycleScope for automatic cancellation on service destruction.
     */
    private fun startAutoSync() {
        if (syncLoopJob?.isActive == true) return
        Log.d(TAG, "Starting auto sync service")
        // Create notification channel
        NotificationHelper.ensureNotificationChannel(
            this,
            Constants.Notification.CHANNEL_ID_AUTO_SYNC,
            Constants.Notification.CHANNEL_NAME_AUTO_SYNC
        )

        // lifecycleScope automatically cancels when service is destroyed
        syncLoopJob = lifecycleScope.launch(Dispatchers.IO) {
            lastSyncTime = System.currentTimeMillis()

            while (isActive) {
                try {
                    checkAndSyncIfNeeded()
                    delay(Constants.AutoSync.CHECK_INTERVAL_MS)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in auto-sync loop: ${e.message}", e)
                    delay(Constants.AutoSync.CHECK_INTERVAL_MS)
                }
            }
        }
    }

    /**
     * Checks if sync conditions are met and triggers sync if needed.
     * Skips if a previous sync cycle is still running.
     */
    private suspend fun checkAndSyncIfNeeded() {
        val currentTime = System.currentTimeMillis()

        // Check if data collection is running
        val dataCollectionStarted = syncTimestampService.getDataCollectionStarted()
        if (dataCollectionStarted == null) {
            return
        }

        // Get auto-sync settings
        val intervalMs = syncTimestampService.getAutoSyncIntervalMs()
        if (intervalMs == Constants.AutoSync.INTERVAL_NONE) {
            return
        }

        // Check if enough time has passed since last sync
        val timeSinceLastSync = currentTime - lastSyncTime
        if (timeSinceLastSync < intervalMs) {
            return
        }

        // Skip if a previous sync is still running
        if (!isSyncing.compareAndSet(false, true)) {
            return
        }

        // Check network conditions
        if (!isNetworkConditionMet()) {
            isSyncing.set(false)
            val networkMode = syncTimestampService.getAutoSyncNetworkMode()
            val networkModeName = when (networkMode) {
                Constants.AutoSync.NETWORK_WIFI_MOBILE -> "WiFi/Mobile"
                Constants.AutoSync.NETWORK_WIFI_ONLY -> "WiFi Only"
                Constants.AutoSync.NETWORK_MOBILE_ONLY -> "Mobile Only"
                else -> "Unknown"
            }
            Log.w(TAG, "Network condition not met (mode=$networkModeName), skipping sync")
            return
        }

        // All conditions met, trigger sync - update lastSyncTime AFTER completion to ensure retry on failure
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                uploadAllSensorData()
                lastSyncTime = System.currentTimeMillis()
            } finally {
                isSyncing.set(false)
            }
        }
    }

    /**
     * Checks if the current network connection meets the configured network preference
     */
    private fun isNetworkConditionMet(): Boolean {
        val networkMode = syncTimestampService.getAutoSyncNetworkMode()

        // If mode is WIFI_MOBILE, any connection is fine
        if (networkMode == Constants.AutoSync.NETWORK_WIFI_MOBILE) {
            return isConnected()
        }

        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        val hasWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        val hasCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)

        return when (networkMode) {
            Constants.AutoSync.NETWORK_WIFI_ONLY -> hasWifi
            Constants.AutoSync.NETWORK_MOBILE_ONLY -> hasCellular
            else -> isConnected()
        }
    }

    /**
     * Checks if device has any network connection
     */
    private fun isConnected(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Uploads all sensor data in parallel for faster sync cycles.
     * Phone sensors and watch sensors are uploaded concurrently.
     */
    private suspend fun uploadAllSensorData() {
        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)
        val skippedCount = AtomicInteger(0)
        val failedSensors = Collections.synchronizedList(mutableListOf<String>())

        try {
            val startTime = System.currentTimeMillis()

            // Launch all phone sensor uploads in parallel
            val phoneJobs = sensors.map { sensor ->
                lifecycleScope.async(Dispatchers.IO) {
                    if (phoneSensorUploadService.hasDataToUpload(sensor.id)) {
                        phoneSensorUploadService.uploadSensorData(sensor.id)
                            .onSuccess { successCount.incrementAndGet() }
                            .onFailure { e ->
                                failureCount.incrementAndGet()
                                failedSensors.add(sensor.name)
                                Log.e(TAG, "Upload failed for ${sensor.name}: ${e.message}", e)
                            }
                    } else {
                        skippedCount.incrementAndGet()
                    }
                }
            }

            // Launch all watch sensor uploads in parallel
            val watchJobs = SensorTypeHelper.watchSensorIds.map { sensorId ->
                lifecycleScope.async(Dispatchers.IO) {
                    if (watchSensorUploadService.hasDataToUpload(sensorId)) {
                        watchSensorUploadService.uploadSensorData(sensorId)
                            .onSuccess { successCount.incrementAndGet() }
                            .onFailure { e ->
                                failureCount.incrementAndGet()
                                failedSensors.add(sensorId)
                                Log.e(TAG, "Upload failed for $sensorId: ${e.message}", e)
                            }
                    } else {
                        skippedCount.incrementAndGet()
                    }
                }
            }

            // Wait for all uploads to complete
            (phoneJobs + watchJobs).awaitAll()

            val elapsed = System.currentTimeMillis() - startTime
            Log.d(
                TAG, "Auto-sync completed in ${elapsed}ms: " +
                        "${successCount.get()} success, ${failureCount.get()} failed, ${skippedCount.get()} skipped"
            )

            // Show notification based on results
            if (successCount.get() > 0) {
                showSuccessNotification(successCount.get())
            } else if (failureCount.get() > 0) {
                showFailureNotification(failureCount.get(), failedSensors)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Fatal error in auto-sync upload: ${e.message}", e)
            showFailureNotification(0, listOf("Fatal error: ${e.message}"))
        }
    }

    /**
     * Shows a success notification when auto-sync completes successfully
     */
    private fun showSuccessNotification(successCount: Int) {
        val pendingIntent =
            NotificationHelper.createMainActivityPendingIntent(
                this,
                Constants.Notification.ID_AUTO_SYNC_SUCCESS
            )
        val localizedContext = LanguageHelper(this).applyLanguage(this)
        val notification = NotificationHelper.buildNotification(
            context = this,
            channelId = Constants.Notification.CHANNEL_ID_AUTO_SYNC,
            title = localizedContext.getString(R.string.auto_sync_success_title),
            text = localizedContext.getString(R.string.auto_sync_success_message, successCount),
            pendingIntent = pendingIntent
        ).build()

        NotificationHelper.showNotification(
            this,
            Constants.Notification.ID_AUTO_SYNC_SUCCESS,
            notification
        )
    }

    /**
     * Shows a failure notification when auto-sync encounters errors
     */
    private fun showFailureNotification(failureCount: Int, failedSensors: List<String>) {
        val failedSensorsText = if (failedSensors.isNotEmpty()) {
            failedSensors.take(3).joinToString(", ") + if (failedSensors.size > 3) "..." else ""
        } else {
            ""
        }

        val pendingIntent =
            NotificationHelper.createMainActivityPendingIntent(
                this,
                Constants.Notification.ID_AUTO_SYNC_FAILURE
            )
        val localizedContext = LanguageHelper(this).applyLanguage(this)
        val notification = NotificationHelper.buildNotification(
            context = this,
            channelId = Constants.Notification.CHANNEL_ID_AUTO_SYNC,
            title = localizedContext.getString(R.string.auto_sync_failure_title),
            text = localizedContext.getString(
                R.string.auto_sync_failure_message,
                failureCount,
                failedSensorsText
            ),
            pendingIntent = pendingIntent
        ).build()

        NotificationHelper.showNotification(
            this,
            Constants.Notification.ID_AUTO_SYNC_FAILURE,
            notification
        )
        Log.w(TAG, "Failure notification shown: $failureCount sensors failed")
    }

}
