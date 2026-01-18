package com.example.survey_test_app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.Service.START_STICKY
import android.app.Service.STOP_FOREGROUND_REMOVE
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import kaist.iclab.tracker.sensor.controller.BackgroundControllerServiceLocator
import kaist.iclab.tracker.sensor.controller.Controller
import kaist.iclab.tracker.sensor.controller.ControllerState
import kaist.iclab.tracker.sensor.core.Sensor
import kaist.iclab.tracker.sensor.core.SensorState
import kaist.iclab.tracker.sensor.survey.SurveySensor
import kaist.iclab.tracker.storage.core.StateStorage
import kaist.iclab.tracker.storage.couchbase.CouchbaseStateStorage
import kotlinx.coroutines.flow.StateFlow
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named
import kotlin.collections.forEach

class MyBackgroundController(
    private val context: Context,
    private val controllerStateStorage: StateStorage<ControllerState>,
    override val sensors: List<Sensor<*, *>>,
    private val serviceNotification: ServiceNotification,
    private val allowPartialSensing: Boolean = false,
) : Controller {
    companion object {
        private val TAG = MyBackgroundController::class.simpleName
    }

    data class ServiceNotification(
        val channelId: String,
        val channelName: String,
        val notificationId: Int,
        val title: String,
        val description: String,
        val icon: Int
    )

    init {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val serviceChannel = NotificationChannel(
            serviceNotification.channelId,
            serviceNotification.channelName,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(serviceChannel)

        sensors.forEach { it.init() }
    }

    override val controllerStateFlow: StateFlow<ControllerState> = controllerStateStorage.stateFlow


    /* Use ForegroundService to collect the data 24/7*/
    private val serviceIntent = Intent(context, ControllerService::class.java)

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun start() {
        context.startForegroundService(serviceIntent)
    }

    override fun stop() {
        if (ControllerService.isServiceRunning) {
            context.stopService(serviceIntent)
        } else {
            sensors.forEach { it.stop() }
            controllerStateStorage.set(ControllerState(ControllerState.FLAG.READY))
        }
    }

    class ControllerService : Service() {
        companion object {
            private val TAG = ControllerService::class.simpleName
            var isServiceRunning = false
        }

        private val stateStorage by inject<StateStorage<ControllerState>>(named("controllerState"))

        private val surveySensor by inject<SurveySensor>()
        private val sensors = listOf(surveySensor)
        private val serviceNotification by inject<ServiceNotification>()
        private val partialSensingAllowed = true

        override fun onBind(intent: Intent?): Binder? = null
        override fun onDestroy() {
            stop()
        }

        private fun run() {
            // Now do the rest of the work after startForeground is called
            if (!(partialSensingAllowed) && sensors.any { it.sensorStateFlow.value.flag == SensorState.FLAG.DISABLED }) {
                stateStorage.set(
                    ControllerState(
                        ControllerState.FLAG.DISABLED,
                        "Some sensors are disabled"
                    )
                )
                throw Exception("Some sensors are disabled")
            }

            stateStorage.set(ControllerState(ControllerState.FLAG.RUNNING))
            sensors.filter { it.sensorStateFlow.value.flag == SensorState.FLAG.ENABLED }
                .forEach { it.start() }
            isServiceRunning = true
        }

        private fun stop() {
            isServiceRunning = false
            stateStorage.set(ControllerState(ControllerState.FLAG.READY))
            sensors.filter { it.sensorStateFlow.value.flag == SensorState.FLAG.RUNNING }
                .forEach { it.stop() }
            stopSelf()
            stopForeground(STOP_FOREGROUND_REMOVE)
        }

        override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
            // CRITICAL: Call startForeground IMMEDIATELY - this must happen within 5 seconds
            try {
                val notificationProps = getNotificationProperties()
                ensureNotificationChannel(notificationProps.channelId)
                val notification = buildNotification(notificationProps)
                val serviceType = getServiceType()

                this.startForeground(notificationProps.notificationId, notification, serviceType)
                run()
            } catch (e: Exception) {
                Log.e(TAG, "Error in onStartCommand", e)
                postEmergencyNotification()
                stop()
            }
            return START_STICKY
        }

        private data class NotificationProperties(
            val channelId: String,
            val notificationId: Int,
            val icon: Int,
            val title: String,
            val description: String
        )

        private fun getNotificationProperties(): NotificationProperties {
            val channelId = try {
                serviceNotification.channelId
            } catch (e: Exception) {
                "default_channel"
            }

            val notificationId = try {
                serviceNotification.notificationId
            } catch (e: Exception) {
                1
            }

            val icon = try {
                serviceNotification.icon
            } catch (e: Exception) {
                android.R.drawable.ic_dialog_info
            }

            val title = try {
                serviceNotification.title
            } catch (e: Exception) {
                "Service"
            }

            val description = try {
                serviceNotification.description
            } catch (e: Exception) {
                "Running"
            }

            return NotificationProperties(channelId, notificationId, icon, title, description)
        }

        private fun ensureNotificationChannel(channelId: String) {
            try {
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (notificationManager.getNotificationChannel(channelId) == null) {
                    val channelName = try {
                        serviceNotification.channelName
                    } catch (e: Exception) {
                        "Default Channel"
                    }
                    val channel = NotificationChannel(
                        channelId,
                        channelName,
                        NotificationManager.IMPORTANCE_DEFAULT
                    )
                    notificationManager.createNotificationChannel(channel)
                }
            } catch (e: Exception) {
                // Channel creation failed, will use default
            }
        }

        private fun buildNotification(props: NotificationProperties): android.app.Notification {
            // Create intent to open the app's main launcher activity
            val packageName = this.applicationContext.packageName
            val launchIntent = this.applicationContext.packageManager.getLaunchIntentForPackage(packageName)
            val pendingIntent = if (launchIntent != null) {
                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                PendingIntent.getActivity(
                    this.applicationContext,
                    0,
                    launchIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            } else {
                null
            }

            val builder = NotificationCompat.Builder(
                this.applicationContext,
                props.channelId
            )
                .setSmallIcon(props.icon)
                .setContentTitle(props.title)
                .setContentText(props.description)
                .setOngoing(true)

            if (pendingIntent != null) {
                builder.setContentIntent(pendingIntent)
            }

            return builder.build()
        }

        private fun getServiceType(): Int {
            val defaultServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }

            return try {
                requiredForegroundServiceType()
            } catch (e: Exception) {
                defaultServiceType
            }
        }

        private fun postEmergencyNotification() {
            try {
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (notificationManager.getNotificationChannel("default_channel") == null) {
                    val channel = NotificationChannel(
                        "default_channel",
                        "Default Channel",
                        NotificationManager.IMPORTANCE_DEFAULT
                    )
                    notificationManager.createNotificationChannel(channel)
                }

                // Create intent to open the app's main launcher activity
                val packageName = this.applicationContext.packageName
                val launchIntent = this.applicationContext.packageManager.getLaunchIntentForPackage(packageName)
                val pendingIntent = if (launchIntent != null) {
                    launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    PendingIntent.getActivity(
                        this.applicationContext,
                        0,
                        launchIntent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                } else {
                    null
                }

                val builder = NotificationCompat.Builder(
                    this.applicationContext,
                    "default_channel"
                )
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("Service")
                    .setContentText("Error occurred")
                    .setOngoing(true)

                if (pendingIntent != null) {
                    builder.setContentIntent(pendingIntent)
                }

                val notification = builder.build()

                val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else {
                    0
                }
                this.startForeground(1, notification, serviceType)
            } catch (e: Exception) {
                // Failed to post emergency notification
            }
        }

        private fun requiredForegroundServiceType(): Int {
            // Allowed service types as declared in AndroidManifest.xml:
            // health|specialUse|location|connectedDevice
            val allowedServiceTypes = setOf(
                ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )

            val defaultServiceType =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
            val sensors = BackgroundControllerServiceLocator.sensors

            // Get all service types from enabled sensors, but filter to only allowed types
            val calculatedTypes = sensors.filter {
                it.sensorStateFlow.value.flag in listOf(
                    SensorState.FLAG.ENABLED,
                    SensorState.FLAG.RUNNING
                )
            }
                .map { it.foregroundServiceTypes.toList() }
                .flatten()
                .toSet()
                .filter { it in allowedServiceTypes } // Only keep types declared in manifest

            // Combine allowed types with default
            return calculatedTypes.fold(defaultServiceType, { acc, type -> acc or type })
        }
    }
}