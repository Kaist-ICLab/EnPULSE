package kaist.iclab.tracker.sensor.galaxywatch

import android.Manifest
import android.content.pm.ServiceInfo
import android.health.connect.HealthPermissions
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

// NOTE: Skin Temperature measurement is available with Galaxy Watch 5 series or later models.

class SkinTemperatureSensor(
    permissionManager: PermissionManager,
    configStorage: StateStorage<Config>,
    private val stateStorage: StateStorage<SensorState>,
    samsungHealthSensorInitializer: SamsungHealthSensorInitializer
) : BaseSensor<SkinTemperatureSensor.Config, SkinTemperatureSensor.Entity>(
    permissionManager, configStorage, stateStorage, Config::class, Entity::class
) {
    override val id: String = "SkinTemperature"
    override val permissions = listOfNotNull(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) HealthPermissions.READ_SKIN_TEMPERATURE else Manifest.permission.BODY_SENSORS,
        // For foreground service type
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && Build.VERSION.SDK_INT <= Build.VERSION_CODES.VANILLA_ICE_CREAM) Manifest.permission.BODY_SENSORS_BACKGROUND else null,
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND else null
    ).toTypedArray()

    override val foregroundServiceTypes: Array<Int> = listOfNotNull(
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH else null
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
        val objectTemperature: Float,
        val ambientTemperature: Float,
        val status: Int
    )

    private val tracker by lazy {
        samsungHealthSensorInitializer.getTracker(HealthTrackerType.SKIN_TEMPERATURE_CONTINUOUS)
    }

    private val listener = samsungHealthSensorInitializer.createDataListener { dataPoints ->
        val timestamp = System.currentTimeMillis()
        val entity = Entity(
            dataPoints.map {
                DataPoint(
                    timestamp,
                    it.timestamp,
                    it.getValue(ValueKey.SkinTemperatureSet.OBJECT_TEMPERATURE),
                    it.getValue(ValueKey.SkinTemperatureSet.AMBIENT_TEMPERATURE),
                    it.getValue(ValueKey.SkinTemperatureSet.STATUS)
                )
            }
        )

        listeners.forEach {
            it.invoke(entity)
        }
    }

    override fun onStart() {
        try {
            tracker.setEventListener(listener)
        } catch (e: UnsupportedOperationException) {
            // Skin Temperature not supported on this device, ignore
        }
    }

    override fun onStop() {
        try {
            tracker.unsetEventListener()
        } catch (e: UnsupportedOperationException) {
            // Skin Temperature not supported on this device, ignore
        }
    }
}
