package kaist.iclab.mobiletracker.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.IBinder
import android.util.Log
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.repository.Result
import kaist.iclab.mobiletracker.services.upload.PhoneSensorUploadService
import kaist.iclab.mobiletracker.services.upload.WatchSensorUploadService
import kaist.iclab.mobiletracker.utils.NotificationHelper
import kaist.iclab.mobiletracker.helpers.LanguageHelper
import kaist.iclab.mobiletracker.utils.SensorTypeHelper
import kaist.iclab.tracker.sensor.core.Sensor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent
import org.koin.core.qualifier.named

/**
 * Service that handles automatic synchronization of sensor data to Supabase
 * based on configured interval and network preferences.
 */
class AutoSyncService : Service(), KoinComponent {
    companion object {
        private const val TAG = "AutoSyncService"
        private const val CHECK_INTERVAL_MS = 10_000L // Check every 10 seconds to catch short intervals
        
        private const val NOTIFICATION_CHANNEL_ID = "auto_sync_channel"
        private const val NOTIFICATION_CHANNEL_NAME = "Auto Sync Notifications"
        private const val NOTIFICATION_ID_SUCCESS = 1001
        private const val NOTIFICATION_ID_FAILURE = 1002
        
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

    private val syncTimestampService: SyncTimestampService by lazy {
        SyncTimestampService(this)
    }
    private val phoneSensorUploadService: PhoneSensorUploadService by inject()
    private val watchSensorUploadService: WatchSensorUploadService by inject()
    private val sensors by inject<List<Sensor<*, *>>>(qualifier = named("sensors"))

    // Coroutine scope tied to service lifecycle
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var syncJob: Job? = null
    private var lastSyncTime: Long = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAutoSync()
        return START_STICKY
    }

    /**
     * Creates notification channel for auto-sync notifications
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for automatic data synchronization"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAutoSync()
        serviceScope.cancel()
    }

    /**
     * Starts the auto-sync coroutine that periodically checks and syncs data
     */
    private fun startAutoSync() {
        if (syncJob?.isActive == true) {
            return
        }

        syncJob = serviceScope.launch {
            lastSyncTime = System.currentTimeMillis()

            while (isActive) {
                try {
                    checkAndSyncIfNeeded()
                    delay(CHECK_INTERVAL_MS)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in auto-sync loop: ${e.message}", e)
                    delay(CHECK_INTERVAL_MS)
                }
            }
        }
    }

    /**
     * Stops the auto-sync coroutine
     */
    private fun stopAutoSync() {
        syncJob?.cancel()
        syncJob = null
    }

    /**
     * Checks if sync conditions are met and triggers sync if needed
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
        if (intervalMs == SyncTimestampService.AUTO_SYNC_INTERVAL_NONE) {
            return
        }

        // Check if enough time has passed since last sync
        val timeSinceLastSync = currentTime - lastSyncTime
        if (timeSinceLastSync < intervalMs) {
            return
        }

        // Check network conditions
        if (!isNetworkConditionMet()) {
            val networkMode = syncTimestampService.getAutoSyncNetworkMode()
            val networkModeName = when (networkMode) {
                SyncTimestampService.AUTO_SYNC_NETWORK_WIFI_MOBILE -> "WiFi/Mobile"
                SyncTimestampService.AUTO_SYNC_NETWORK_WIFI_ONLY -> "WiFi Only"
                SyncTimestampService.AUTO_SYNC_NETWORK_MOBILE_ONLY -> "Mobile Only"
                else -> "Unknown"
            }
            Log.w(TAG, "Network condition not met (mode=$networkModeName), skipping sync")
            return
        }

        // All conditions met, trigger sync
        serviceScope.launch {
            uploadAllSensorData()
        }
        lastSyncTime = currentTime
    }

    /**
     * Checks if the current network connection meets the configured network preference
     */
    private fun isNetworkConditionMet(): Boolean {
        val networkMode = syncTimestampService.getAutoSyncNetworkMode()
        
        // If mode is WIFI_MOBILE, any connection is fine
        if (networkMode == SyncTimestampService.AUTO_SYNC_NETWORK_WIFI_MOBILE) {
            return isConnected()
        }

        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        val hasWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        val hasCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)

        return when (networkMode) {
            SyncTimestampService.AUTO_SYNC_NETWORK_WIFI_ONLY -> hasWifi
            SyncTimestampService.AUTO_SYNC_NETWORK_MOBILE_ONLY -> hasCellular
            else -> isConnected()
        }
    }

    /**
     * Checks if device has any network connection
     */
    private fun isConnected(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Uploads all sensor data for all phone sensors
     */
    private suspend fun uploadAllSensorData() {
        var successCount = 0
        var failureCount = 0
        var skippedCount = 0
        val failedSensors = mutableListOf<String>()
        
        try {
            sensors.forEach { sensor ->
                if (phoneSensorUploadService.hasDataToUpload(sensor.id)) {
                    when (val result = phoneSensorUploadService.uploadSensorData(sensor.id)) {
                        is Result.Success -> {
                            successCount++
                        }
                        is Result.Error -> {
                            failureCount++
                            failedSensors.add(sensor.name)
                            Log.e(TAG, "Error uploading data for ${sensor.name}: ${result.message}", result.exception)
                        }
                    }
                } else {
                    skippedCount++
                }
            }
            
            // Upload all watch sensors
            SensorTypeHelper.watchSensorIds.forEach { sensorId ->
                if (watchSensorUploadService.hasDataToUpload(sensorId)) {
                    when (val result = watchSensorUploadService.uploadSensorData(sensorId)) {
                        is Result.Success -> {
                            successCount++
                        }
                        is Result.Error -> {
                            failureCount++
                            failedSensors.add(sensorId)
                            Log.e(TAG, "Error uploading watch data for $sensorId: ${result.message}", result.exception)
                        }
                    }
                } else {
                    skippedCount++
                }
            }
            
            // Show notification based on results (show success OR failure, not both)
            if (successCount > 0) {
                showSuccessNotification(successCount)
            } else if (failureCount > 0) {
                showFailureNotification(failureCount, failedSensors)
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
        val pendingIntent = NotificationHelper.createMainActivityPendingIntent(this, NOTIFICATION_ID_SUCCESS)
        val localizedContext = LanguageHelper(this).applyLanguage(this)
        val notification = NotificationHelper.buildNotification(
            context = this,
            channelId = NOTIFICATION_CHANNEL_ID,
            title = localizedContext.getString(R.string.auto_sync_success_title),
            text = localizedContext.getString(R.string.auto_sync_success_message, successCount),
            pendingIntent = pendingIntent
        ).build()
        
        NotificationHelper.showNotification(this, NOTIFICATION_ID_SUCCESS, notification)
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
        
        val pendingIntent = NotificationHelper.createMainActivityPendingIntent(this, NOTIFICATION_ID_FAILURE)
        val localizedContext = LanguageHelper(this).applyLanguage(this)
        val notification = NotificationHelper.buildNotification(
            context = this,
            channelId = NOTIFICATION_CHANNEL_ID,
            title = localizedContext.getString(R.string.auto_sync_failure_title),
            text = localizedContext.getString(R.string.auto_sync_failure_message, failureCount, failedSensorsText),
            pendingIntent = pendingIntent
        ).build()
        
        NotificationHelper.showNotification(this, NOTIFICATION_ID_FAILURE, notification)
        Log.w(TAG, "Failure notification shown: $failureCount sensors failed")
    }

}
