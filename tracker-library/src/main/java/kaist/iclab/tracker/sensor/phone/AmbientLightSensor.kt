package kaist.iclab.tracker.sensor.phone

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LightMode
import kaist.iclab.tracker.R
import kaist.iclab.tracker.permission.PermissionManager
import kaist.iclab.tracker.sensor.core.BaseSensor
import kaist.iclab.tracker.sensor.core.SensorConfig
import kaist.iclab.tracker.sensor.core.SensorEntity
import kaist.iclab.tracker.sensor.core.SensorState
import kaist.iclab.tracker.storage.core.StateStorage
import kotlinx.serialization.Serializable
import java.util.concurrent.TimeUnit

class AmbientLightSensor(
    context: Context,
    permissionManager: PermissionManager,
    configStorage: StateStorage<Config>,
    stateStorage: StateStorage<SensorState>,
) : BaseSensor<AmbientLightSensor.Config, AmbientLightSensor.Entity>(
    permissionManager, configStorage, stateStorage, Config::class, Entity::class,
    titleResId = R.string.sensor_ambient_light,
    descriptionResId = R.string.sensor_desc_ambient_light,
    icon = Icons.Default.LightMode
) {

    data class Config(
        val interval: Long
    ) : SensorConfig

    @Serializable
    data class Entity(
        val received: Long,
        val timestamp: Long,
        val accuracy: Int,
        val value: Float
    ) : SensorEntity()

    override val permissions = listOfNotNull<String>().toTypedArray()
    override val foregroundServiceTypes: Array<Int> = listOfNotNull<Int>().toTypedArray()

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager


    private val sensorEventListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor?, p1: Int) {}
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                val timestamp = System.currentTimeMillis()

                listeners.forEach { listener ->
                    listener.invoke(
                        Entity(
                            timestamp,
                            timestamp,
                            it.accuracy,
                            it.values[0]
                        )
                    )
                }
            }
        }
    }

    override fun init() {
        super.init()
        if (sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) == null) {
            SensorState(SensorState.FLAG.UNAVAILABLE, "AmbientLight Sensor is not available")
        }
    }

    override fun onStart() {

        sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)?.let { sensor ->
            sensorManager.registerListener(
                sensorEventListener,
                sensor,
                TimeUnit.MILLISECONDS.toMicros(configStateFlow.value.interval).toInt()
            )
        }
    }

    override fun onStop() {
        sensorManager.unregisterListener(sensorEventListener)
    }
}