package kaist.iclab.tracker.sensor.controller

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import kaist.iclab.tracker.sensor.core.Sensor
import kaist.iclab.tracker.sensor.core.SensorState
import kaist.iclab.tracker.storage.core.StateStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class BackgroundController(
    private val context: Context,
    private val controllerStateStorage: StateStorage<ControllerState>,
    override val sensors: List<Sensor<*, *>>,
    private val serviceNotification: ServiceNotification,
    private val allowPartialSensing: Boolean = false,
) : Controller {
    companion object {
        private val TAG = BackgroundController::class.simpleName
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

        BackgroundControllerServiceLocator.controllerStateStorage = controllerStateStorage
        BackgroundControllerServiceLocator.sensors = sensors
        BackgroundControllerServiceLocator.serviceNotification = serviceNotification
        BackgroundControllerServiceLocator.allowPartialSensing = allowPartialSensing
        BackgroundControllerServiceLocator.offBodyDetector = OffBodyDetector(context)
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
            if (controllerStateStorage.get().flag != ControllerState.FLAG.READY) {
                controllerStateStorage.set(ControllerState(ControllerState.FLAG.READY))
            }
        }
    }

    class ControllerService : Service() {
        companion object {
            private val TAG = ControllerService::class.simpleName
            var isServiceRunning = false
        }

        private lateinit var stateStorage: StateStorage<ControllerState>
        private lateinit var sensors: List<Sensor<*, *>>
        private lateinit var serviceNotification: ServiceNotification
        private var partialSensingAllowed: Boolean = false

        private lateinit var offBodyDetector: OffBodyDetector
        private var serviceScope: CoroutineScope? = null
        private var offBodyJob: Job? = null

        override fun onCreate() {
            super.onCreate()
            resolveDependencies()
        }

        override fun onBind(intent: Intent?): Binder? = null
        override fun onDestroy() {
            stop()
        }

        private fun resolveDependencies() {
            val provider = application as? BackgroundControllerDependenciesProvider
            val dependencies = provider?.provideBackgroundControllerDependencies()

            if (dependencies != null) {
                stateStorage = dependencies.controllerStateStorage
                sensors = dependencies.sensors
                serviceNotification = dependencies.serviceNotification
                partialSensingAllowed = dependencies.allowPartialSensing
                offBodyDetector = dependencies.offBodyDetector
                return
            }

            // Fallback for callers that still rely on the process-local locator.
            stateStorage = BackgroundControllerServiceLocator.controllerStateStorage
            sensors = BackgroundControllerServiceLocator.sensors
            serviceNotification = BackgroundControllerServiceLocator.serviceNotification
            partialSensingAllowed = BackgroundControllerServiceLocator.allowPartialSensing
            offBodyDetector = BackgroundControllerServiceLocator.offBodyDetector
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

            // Start off-body detection and observe wrist state
            offBodyDetector.start()
            serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
            offBodyJob = serviceScope?.launch {
                offBodyDetector.isOnWrist.collectLatest { isWorn ->
                    val currentFlag = stateStorage.get().flag
                    if (!isWorn && currentFlag == ControllerState.FLAG.RUNNING) {
                        pauseSensors()
                    } else if (isWorn && currentFlag == ControllerState.FLAG.PAUSED) {
                        resumeSensors()
                    }
                }
            }
        }

        /**
         * Pause all running sensors because the watch is not being worn.
         * The foreground service stays alive to keep the off-body listener active.
         */
        private fun pauseSensors() {
            Log.i(TAG, "Pausing sensors — watch not worn")
            sensors.filter { it.sensorStateFlow.value.flag == SensorState.FLAG.RUNNING }
                .forEach { it.stop() }
            stateStorage.set(ControllerState(ControllerState.FLAG.PAUSED, "Watch not worn"))
        }

        /**
         * Resume sensors that were paused because the watch was not worn.
         */
        private fun resumeSensors() {
            Log.i(TAG, "Resuming sensors — watch is worn again")
            stateStorage.set(ControllerState(ControllerState.FLAG.RUNNING))
            sensors.filter { it.sensorStateFlow.value.flag == SensorState.FLAG.ENABLED }
                .forEach { it.start() }
        }

        private fun stop() {
            // Clean up off-body detection
            offBodyJob?.cancel()
            offBodyJob = null
            serviceScope?.cancel()
            serviceScope = null
            if (::offBodyDetector.isInitialized) {
                offBodyDetector.stop()
            }

            isServiceRunning = false
            stateStorage.set(ControllerState(ControllerState.FLAG.READY))
            sensors.filter {
                it.sensorStateFlow.value.flag == SensorState.FLAG.RUNNING
            }.forEach { it.stop() }
            stopSelf()
            stopForeground(STOP_FOREGROUND_REMOVE)
        }

        override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
            // CRITICAL: Call startForeground IMMEDIATELY - this must happen within 5 seconds
            try {
                if (!::stateStorage.isInitialized || !::serviceNotification.isInitialized) {
                    resolveDependencies()
                }
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
            } catch (_: Exception) {
                "default_channel"
            }

            val notificationId = try {
                serviceNotification.notificationId
            } catch (_: Exception) {
                1
            }

            val icon = try {
                serviceNotification.icon
            } catch (_: Exception) {
                android.R.drawable.ic_dialog_info
            }

            val title = try {
                serviceNotification.title
            } catch (_: Exception) {
                "Service"
            }

            val description = try {
                serviceNotification.description
            } catch (_: Exception) {
                "Running"
            }

            return NotificationProperties(channelId, notificationId, icon, title, description)
        }

        private fun ensureNotificationChannel(channelId: String) {
            try {
                val notificationManager =
                    getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                if (notificationManager.getNotificationChannel(channelId) == null) {
                    val channelName = try {
                        serviceNotification.channelName
                    } catch (_: Exception) {
                        "Default Channel"
                    }
                    val channel = NotificationChannel(
                        channelId,
                        channelName,
                        NotificationManager.IMPORTANCE_DEFAULT
                    )
                    notificationManager.createNotificationChannel(channel)
                }
            } catch (_: Exception) {
                // Channel creation failed, will use default
            }
        }

        private fun buildNotification(props: NotificationProperties): android.app.Notification {
            // Create intent to open the app's main launcher activity
            val packageName = this.applicationContext.packageName
            val launchIntent =
                this.applicationContext.packageManager.getLaunchIntentForPackage(packageName)
            val pendingIntent = if (launchIntent != null) {
                launchIntent.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
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
            val defaultServiceType =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else {
                    0
                }

            return try {
                requiredForegroundServiceType()
            } catch (_: Exception) {
                defaultServiceType
            }
        }

        private fun postEmergencyNotification() {
            try {
                val notificationManager =
                    getSystemService(NOTIFICATION_SERVICE) as NotificationManager
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
                val launchIntent =
                    this.applicationContext.packageManager.getLaunchIntentForPackage(packageName)
                val pendingIntent = if (launchIntent != null) {
                    launchIntent.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
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

                val serviceType =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    } else {
                        0
                    }
                this.startForeground(1, notification, serviceType)
            } catch (_: Exception) {
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
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )

            val defaultServiceType =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0

            // Get all service types from enabled sensors, but filter to only allowed types
            val calculatedTypes = sensors.asSequence().filter {
                it.sensorStateFlow.value.flag in listOf(
                    SensorState.FLAG.ENABLED,
                    SensorState.FLAG.RUNNING
                )
            }.flatMap { it.foregroundServiceTypes.toList() }
                .toSet()
                .filter { it in allowedServiceTypes }.toList() // Only keep types declared in manifest

            // Combine allowed types with default
            return calculatedTypes.fold(defaultServiceType) { acc, type -> acc or type }
        }
    }
}
