package kaist.iclab.mobiletracker.services

import kotlinx.coroutines.CancellationException
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kaist.iclab.mobiletracker.Constants
import kaist.iclab.mobiletracker.repository.onFailure
import kaist.iclab.mobiletracker.services.upload.DataUploadService
import kaist.iclab.mobiletracker.services.upload.WebAppLogUploader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.milliseconds

/**
 * Service that decides WHEN sensor data should be synced to Supabase, based on the configured
 * interval and network preferences. The actual upload work — shared with the manual "Upload Now"
 * action — is delegated to [DataUploadService], a foreground service with its own progress
 * notification, so this service never has to become foreground itself.
 *
 * Uses LifecycleService for automatic coroutine lifecycle management.
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
    private val webAppLogUploader by inject<WebAppLogUploader>()

    private val isoFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    private var lastSyncTime: Long = 0

    /** Guards against duplicate sync loops from repeated onStartCommand calls */
    private var syncLoopJob: Job? = null

    /** Independent loop for webapp logs; see [startWebAppLogSync] */
    private var webAppLogSyncJob: Job? = null

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
        startWebAppLogSync()
        if (syncLoopJob?.isActive == true) return
        Log.d(TAG, "Starting auto sync service")

        // lifecycleScope automatically cancels when service is destroyed
        syncLoopJob = lifecycleScope.launch(Dispatchers.IO) {
            lastSyncTime = System.currentTimeMillis()

            while (isActive) {
                try {
                    checkAndSyncIfNeeded()
                    delay(Constants.AutoSync.CHECK_INTERVAL_MS.milliseconds)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.e(TAG, "Error in auto-sync loop: ${e.message}", e)
                    delay(Constants.AutoSync.CHECK_INTERVAL_MS.milliseconds)
                }
            }
        }
    }

    /**
     * WebApp logs are not sensor data, so they drain on their own loop instead of through
     * [checkAndSyncIfNeeded]: no `getDataCollectionStarted()` gate and no campaign active-sensor
     * check, so a webapp keeps logging — and its logs keep reaching Supabase — while sensor
     * collection is stopped. The user's network preference is still honoured.
     */
    private fun startWebAppLogSync() {
        if (webAppLogSyncJob?.isActive == true) return

        webAppLogSyncJob = lifecycleScope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    if (isNetworkConditionMet()) {
                        webAppLogUploader.flush().onFailure { e ->
                            Log.e(TAG, "WebApp log upload failed: ${e.message}", e)
                        }
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.e(TAG, "Error in webapp log sync loop: ${e.message}", e)
                }
                delay(Constants.AutoSync.CHECK_INTERVAL_MS.milliseconds)
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
        syncTimestampService.getDataCollectionStarted() ?: return

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

        // All conditions met — hand the actual upload off to DataUploadService's foreground
        // service (shared with manual "Upload Now") and update lastSyncTime AFTER it completes,
        // to ensure a retry on failure.
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                DataUploadService.start(applicationContext).await()
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
            getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
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
            getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
