package kaist.iclab.tracker.sensor.phone

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import kaist.iclab.tracker.listener.BroadcastListener
import kaist.iclab.tracker.permission.PermissionManager
import kaist.iclab.tracker.sensor.core.BaseSensor
import kaist.iclab.tracker.sensor.core.SensorConfig
import kaist.iclab.tracker.sensor.core.SensorEntity
import kaist.iclab.tracker.sensor.core.SensorState
import kaist.iclab.tracker.storage.core.StateStorage
import kotlinx.serialization.Serializable

class ScreenSensor(
    context: Context,
    permissionManager: PermissionManager,
    configStorage: StateStorage<Config>,
    private val stateStorage: StateStorage<SensorState>,
) : BaseSensor<ScreenSensor.Config, ScreenSensor.Entity>(
    permissionManager, configStorage, stateStorage, Config::class, Entity::class
) {
    /*No attribute required... can not be data class*/
    class Config : SensorConfig

    @Serializable
    data class Entity(
        val received: Long,
        val timestamp: Long,
        val type: String,
    ) : SensorEntity()

    override val permissions = listOfNotNull(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) Manifest.permission.FOREGROUND_SERVICE_SPECIAL_USE else null
    ).toTypedArray()

    override val foregroundServiceTypes: Array<Int> = listOfNotNull(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else null
    ).toTypedArray()

    private val broadcastListener = BroadcastListener(
        context,
        arrayOf(
            Intent.ACTION_SCREEN_ON,
            Intent.ACTION_SCREEN_OFF,
            Intent.ACTION_USER_PRESENT
        )
    )

    /**
     * Maps Android intent action strings to shorter, more readable values
     */
    private fun mapActionToType(action: String?): String {
        return when (action) {
            Intent.ACTION_SCREEN_ON -> "SCREEN_ON"
            Intent.ACTION_SCREEN_OFF -> "SCREEN_OFF"
            Intent.ACTION_USER_PRESENT -> "USER_PRESENT"
            else -> action ?: "UNKNOWN"
        }
    }

    private val mainCallback = { intent: Intent? ->
        listeners.forEach { listener ->
            val timestamp = System.currentTimeMillis()
            listener.invoke(
                Entity(
                    timestamp,
                    timestamp,
                    mapActionToType(intent?.action)
                )
            )
        }

    }

    override fun onStart() {
        broadcastListener.addListener(mainCallback)
    }

    override fun onStop() {
        broadcastListener.removeListener(mainCallback)
    }
}