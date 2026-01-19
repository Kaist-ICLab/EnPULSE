package kaist.iclab.tracker.sensor.phone

import android.Manifest
import android.content.pm.ServiceInfo
import android.os.Build
import kaist.iclab.tracker.listener.NotificationListener
import kaist.iclab.tracker.listener.core.NotificationEventInfo
import kaist.iclab.tracker.permission.PermissionManager
import kaist.iclab.tracker.sensor.core.BaseSensor
import kaist.iclab.tracker.sensor.core.SensorConfig
import kaist.iclab.tracker.sensor.core.SensorEntity
import kaist.iclab.tracker.sensor.core.SensorState
import kaist.iclab.tracker.storage.core.StateStorage
import kotlinx.serialization.Serializable

class NotificationSensor(
    permissionManager: PermissionManager,
    configStorage: StateStorage<Config>,
    private val stateStorage: StateStorage<SensorState>,
) : BaseSensor<NotificationSensor.Config, NotificationSensor.Entity>(
    permissionManager, configStorage, stateStorage, Config::class, Entity::class
) {
    /*No attribute required... can not be data class*/
    class Config : SensorConfig

    @Serializable
    data class Entity(
        val received: Long,
        val timestamp: Long,
        val packageName: String,
        val eventType: String,
        val title: String,
        val text: String,
        val visibility: Int,
        val category: String
    ) : SensorEntity()

    override val permissions =
        listOfNotNull(
            Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) Manifest.permission.FOREGROUND_SERVICE_SPECIAL_USE else null
        ).toTypedArray()

    override val foregroundServiceTypes: Array<Int> = listOfNotNull(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else null
    ).toTypedArray()

    private val notificationListener = NotificationListener()

    private val mainCallback = { e: NotificationEventInfo ->
        val eventType = when (e) {
            is NotificationEventInfo.Posted -> "POSTED"
            is NotificationEventInfo.Removed -> "REMOVED"
        }

        val notification = e.sbn?.notification
        val entity = Entity(
            System.currentTimeMillis(),
            (e.sbn?.postTime ?: 0),
            e.sbn?.packageName ?: "",
            eventType,
            notification?.extras?.getString("android.title") ?: "",
            notification?.extras?.getString("android.text") ?: "",
            notification?.visibility ?: -1,
            notification?.category ?: ""
        )

        listeners.forEach { listener ->
            listener.invoke(entity)
        }
    }

//    override fun init() {
//        super.init()
////        if (context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)) {
////            stateStorage.set(SensorState(SensorState.FLAG.DISABLED))
////        } else {
////            stateStorage.set(SensorState(SensorState.FLAG.UNAVAILABLE, "Notification access is not granted."))
////        }
//    }

    override fun onStart() {
        notificationListener.addListener(mainCallback)
    }

    override fun onStop() {
        notificationListener.removeListener(mainCallback)
    }
}