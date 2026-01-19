package kaist.iclab.mobiletracker.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.helpers.LanguageHelper
import kaist.iclab.mobiletracker.utils.NotificationHelper
import kaist.iclab.mobiletracker.repository.PhoneSensorRepository
import kaist.iclab.mobiletracker.repository.Result
import kaist.iclab.tracker.sensor.controller.BackgroundController
import kaist.iclab.tracker.sensor.core.Sensor
import kaist.iclab.tracker.sensor.core.SensorEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent
import org.koin.core.qualifier.named

/**
 * Foreground service for receiving and storing phone sensor data locally in Room database.
 * 
 * This service runs in the foreground and listens to sensor data from the tracker library,
 * then stores it using PhoneSensorRepository. It handles local storage only.
 * 
 * For remote storage (Supabase upload), see the watch sensor services in this package.
 */
class PhoneSensorDataService : Service(), KoinComponent {
    companion object {
        private const val TAG = "PhoneSensorDataService"
        
        /**
         * Helper function to start the service from a Context
         */
        fun start(context: Context) {
            val intent = Intent(context, PhoneSensorDataService::class.java)
            context.startForegroundService(intent)
        }
        
        /**
         * Helper function to stop the service from a Context
         */
        fun stop(context: Context) {
            val intent = Intent(context, PhoneSensorDataService::class.java)
            context.stopService(intent)
        }
    }
    
    private val sensors by inject<List<Sensor<*, *>>>(qualifier = named("phoneSensors"))
    private val phoneSensorRepository by inject<PhoneSensorRepository>()
    private val serviceNotification by inject<BackgroundController.ServiceNotification>()
    private val timestampService: SyncTimestampService by lazy { SyncTimestampService(this) }

    // Coroutine scope tied to service lifecycle
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val listener: Map<String, (SensorEntity) -> Unit> = sensors.associate { it.id to
        { e: SensorEntity ->
            // NOTE: Uncomment this if you want to verify the data is received
            Log.v(TAG, "[PHONE] - Data received from ${it.name}: $e")

            if (phoneSensorRepository.hasStorageForSensor(it.id)) {
                serviceScope.launch {
                    when (val result = phoneSensorRepository.insertSensorData(it.id, e)) {
                        is Result.Success -> {
                            // Track when phone sensor data is collected
                            timestampService.updateLastPhoneSensorData()
                        }
                        is Result.Error -> {
                            Log.e(TAG, "[PHONE] - Failed to store data from ${it.name}: ${result.message}", result.exception)
                        }
                    }
                }
            } else {
                Log.w(TAG, "[PHONE] - No storage found for sensor ${it.name} (${it.id})")
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val pendingIntent = NotificationHelper.createMainActivityPendingIntent(this, 0)
        
        // Get localized strings for notification
        val localizedContext = LanguageHelper(this).applyLanguage(this)
        
        val postNotification = NotificationHelper.buildNotification(
            context = this,
            channelId = serviceNotification.channelId,
            title = localizedContext.getString(R.string.notification_title),
            text = localizedContext.getString(R.string.notification_description),
            smallIcon = serviceNotification.icon,
            ongoing = true,
            pendingIntent = pendingIntent
        ).build()

        val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }

        this.startForeground(
            serviceNotification.notificationId,
            postNotification,
            serviceType
        )

        // Remove listeners first to prevent duplicates if onStartCommand is called multiple times
        for (sensor in sensors) {
            sensor.removeListener(listener[sensor.id]!!)
        }

        // Then add listeners
        for (sensor in sensors) {
            sensor.addListener(listener[sensor.id]!!)
        }

        return START_STICKY
    }

    override fun onDestroy() {
        // Cancel all coroutines when service is destroyed
        serviceScope.cancel()

        // Remove all sensor listeners
        for (sensor in sensors) {
            sensor.removeListener(listener[sensor.id]!!)
        }
        
        super.onDestroy()
    }
}

