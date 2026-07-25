package kaist.iclab.tracker.sensor.phone

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import kaist.iclab.tracker.listener.BroadcastListener
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SettingsSuggest
import kaist.iclab.tracker.R
import kaist.iclab.tracker.permission.PermissionManager
import kaist.iclab.tracker.sensor.core.BaseSensor
import kaist.iclab.tracker.sensor.core.SensorConfig
import kaist.iclab.tracker.sensor.core.SensorEntity
import kaist.iclab.tracker.sensor.core.SensorState
import kaist.iclab.tracker.storage.core.StateStorage
import kotlinx.serialization.Serializable

class DeviceModeSensor(
    context: Context,
    permissionManager: PermissionManager,
    configStorage: StateStorage<Config>,
    stateStorage: StateStorage<SensorState>,
) : BaseSensor<DeviceModeSensor.Config, DeviceModeSensor.Entity>(
    permissionManager, configStorage, stateStorage, Config::class, Entity::class,
    titleResId = R.string.sensor_device_mode,
    descriptionResId = R.string.sensor_desc_device_mode,
    icon = Icons.Default.SettingsSuggest
) {
    companion object {
        // Notification mode
        const val NOTIFICATION_MODE_EVENT = "NOTIFICATION_MODE_EVENT"
        const val NOTIFICATION_MODE_FILTER_ALARMS = "NOTIFICATION_MODE_FILTER_ALARMS"
        const val NOTIFICATION_MODE_FILTER_ALL = "NOTIFICATION_MODE_FILTER_ALL"
        const val NOTIFICATION_MODE_FILTER_NONE = "NOTIFICATION_MODE_FILTER_NONE"
        const val NOTIFICATION_MODE_FILTER_PRIORITY = "NOTIFICATION_MODE_FILTER_PRIORITY"
        const val NOTIFICATION_MODE_FILTER_UNKNOWN = "NOTIFICATION_MODE_FILTER_UNKNOWN"

        // Power save mode
        const val POWER_SAVE_MODE_EVENT = "POWER_SAVE_MODE_EVENT"
        const val POWER_SAVE_MODE_ON = "POWER_SAVE_MODE_ON"
        const val POWER_SAVE_MODE_OFF = "POWER_SAVE_MODE_OFF"

        // Airplane mode
        const val AIRPLANE_MODE_EVENT = "AIRPLANE_MODE_EVENT"
        const val AIRPLANE_MODE_ON = "AIRPLANE_MODE_ON"
        const val AIRPLANE_MODE_OFF = "AIRPLANE_MODE_OFF"
    }

    class Config : SensorConfig

    @Serializable
    data class Entity(
        val received: Long,
        val timestamp: Long,
        val eventType: String,
        val value: String,
    ) : SensorEntity()

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    override val permissions = listOfNotNull<String>().toTypedArray()

    override val foregroundServiceTypes = listOfNotNull<Int>().toTypedArray()

    private val broadcastListener = BroadcastListener(
        context,
        arrayOf(
            Intent.ACTION_AIRPLANE_MODE_CHANGED,
            NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED,
            PowerManager.ACTION_POWER_SAVE_MODE_CHANGED,
        )
    )

    private val mainCallback = mainCallback@{ intent: Intent? ->
        if (intent == null) return@mainCallback

        val timestamp = System.currentTimeMillis()
        val something = when (intent.action) {
            NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED -> {
                Entity(
                    timestamp,
                    timestamp,
                    NOTIFICATION_MODE_EVENT,
                    when (notificationManager.currentInterruptionFilter) {
                        NotificationManager.INTERRUPTION_FILTER_ALARMS -> NOTIFICATION_MODE_FILTER_ALARMS
                        NotificationManager.INTERRUPTION_FILTER_ALL -> NOTIFICATION_MODE_FILTER_ALL
                        NotificationManager.INTERRUPTION_FILTER_NONE -> NOTIFICATION_MODE_FILTER_NONE
                        NotificationManager.INTERRUPTION_FILTER_PRIORITY -> NOTIFICATION_MODE_FILTER_PRIORITY
                        NotificationManager.INTERRUPTION_FILTER_UNKNOWN -> NOTIFICATION_MODE_FILTER_UNKNOWN
                        else -> throw Exception()
                    }
                )

            }

            PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> {
                Entity(
                    timestamp,
                    timestamp,
                    POWER_SAVE_MODE_EVENT,
                    if (powerManager.isPowerSaveMode) POWER_SAVE_MODE_ON else POWER_SAVE_MODE_OFF
                )
            }

            Intent.ACTION_AIRPLANE_MODE_CHANGED -> {
                Entity(
                    timestamp,
                    timestamp,
                    AIRPLANE_MODE_EVENT,
                    if (intent.getBooleanExtra(
                            "state",
                            true
                        )
                    ) AIRPLANE_MODE_ON else AIRPLANE_MODE_OFF
                )
            }

            else -> null
        }

        if (something == null) return@mainCallback
        listeners.forEach { listener ->
            listener.invoke(something)
        }
    }

    override fun onStart() {
        broadcastListener.addListener(mainCallback)
    }

    override fun onStop() {
        broadcastListener.removeListener(mainCallback)
    }
}