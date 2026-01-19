package kaist.iclab.tracker.sensor.galaxywatch

import android.Manifest
import android.content.pm.ServiceInfo
import android.os.Build
import com.samsung.android.service.health.tracking.data.HealthTrackerType
import com.samsung.android.service.health.tracking.data.ValueKey
import kaist.iclab.tracker.listener.SamsungHealthSensorInitializer
import kaist.iclab.tracker.permission.PermissionManager
import kaist.iclab.tracker.sensor.core.BaseSensor
import kaist.iclab.tracker.sensor.core.SensorConfig
import kaist.iclab.tracker.sensor.core.SensorEntity
import kaist.iclab.tracker.sensor.core.SensorState
import kaist.iclab.tracker.storage.core.StateStorage
import kotlinx.serialization.Serializable

class AccelerometerSensor(
    permissionManager: PermissionManager,
    configStorage: StateStorage<Config>,
    private val stateStorage: StateStorage<SensorState>,
    samsungHealthSensorInitializer: SamsungHealthSensorInitializer
) : BaseSensor<AccelerometerSensor.Config, AccelerometerSensor.Entity>(
    permissionManager, configStorage, stateStorage, Config::class, Entity::class
) {
    override val id: String = "Accelerometer"
    override val permissions = listOfNotNull(
        Manifest.permission.ACTIVITY_RECOGNITION,
        // Unable to call health foreground service using activity recognition after BAKLAVA
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.VANILLA_ICE_CREAM) Manifest.permission.BODY_SENSORS else null,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && Build.VERSION.SDK_INT <= Build.VERSION_CODES.VANILLA_ICE_CREAM) Manifest.permission.BODY_SENSORS_BACKGROUND else null,
//        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND else null
    ).toTypedArray()

    override val foregroundServiceTypes: Array<Int> = listOfNotNull(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && Build.VERSION.SDK_INT <= Build.VERSION_CODES.VANILLA_ICE_CREAM) ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH else null
    ).toTypedArray()

    /*No attribute required... can not be data class*/
    class Config : SensorConfig

    override val initialConfig: Config = Config()

    @Serializable
    data class Entity(
        val dataPoint: List<DataPoint>
    ) : SensorEntity()

    @Serializable
    data class DataPoint(
        val received: Long,
        val timestamp: Long,
        val x: Float,
        val y: Float,
        val z: Float
    )


    private val tracker by lazy {
        samsungHealthSensorInitializer.getTracker(HealthTrackerType.ACCELEROMETER_CONTINUOUS)
    }

    private val listener = samsungHealthSensorInitializer.createDataListener { dataPoints ->
        val timestamp = System.currentTimeMillis()
        val entity = Entity(
            dataPoints.map {
                DataPoint(
                    timestamp,
                    it.timestamp,
                    rawDataToSI(it.getValue(ValueKey.AccelerometerSet.ACCELEROMETER_X)),
                    rawDataToSI(it.getValue(ValueKey.AccelerometerSet.ACCELEROMETER_Y)),
                    rawDataToSI(it.getValue(ValueKey.AccelerometerSet.ACCELEROMETER_Z))
                )
            }
        )

        listeners.forEach {
            it.invoke(entity)
        }
    }

    private fun rawDataToSI(value: Int): Float {
        return 9.81F / (16383.75F / 4.0F) * value
    }

    override fun onStart() {
        tracker.setEventListener(listener)
    }

    override fun onStop() {
        tracker.unsetEventListener()
    }
}