package kaist.iclab.tracker.sensor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Science
import kaist.iclab.tracker.R
import kaist.iclab.tracker.listener.SampleListener
import kaist.iclab.tracker.permission.PermissionManager
import kaist.iclab.tracker.sensor.core.BaseSensor
import kaist.iclab.tracker.sensor.core.SensorConfig
import kaist.iclab.tracker.sensor.core.SensorEntity
import kaist.iclab.tracker.sensor.core.SensorState
import kaist.iclab.tracker.storage.core.StateStorage
import kotlinx.serialization.Serializable

/**
 * This is just an example that shows the general structure of Sensors,
 * and how custom sensors should be implemented.
 */

class SampleSensor(
    permissionManager: PermissionManager,
    configStorage: StateStorage<Config>,
    stateStorage: StateStorage<SensorState>,
    override val initialConfig: Config
) : BaseSensor<SampleSensor.Config, SampleSensor.Entity>(
    permissionManager, configStorage, stateStorage, Config::class, Entity::class,
    titleResId = R.string.sensor_activity_recognition, // Placeholder
    descriptionResId = R.string.sensor_desc_default,
    icon = Icons.Default.Science
) {
    override val permissions: Array<String> = listOfNotNull<String>().toTypedArray()
    override val foregroundServiceTypes: Array<Int> = listOfNotNull<Int>().toTypedArray()

    data class Config(
        val interval: Long
    ) : SensorConfig

    @Serializable
    data class Entity(
        val timestamp: Long,
    ) : SensorEntity()

    override fun init() {}

    private var listener: SampleListener? = null
    private val handleInvoke: (timestamp: Long) -> Unit = { timestamp ->
        listeners.forEach { it.invoke(Entity(timestamp)) }
    }

    override fun onStop() {
        listener?.removeListener(handleInvoke)
        listener = null
    }

    override fun onStart() {
        listener = SampleListener(configStateFlow.value.interval)
        listener?.addListener(handleInvoke)
    }
}