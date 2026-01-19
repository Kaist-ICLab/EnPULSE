package kaist.iclab.tracker.sensor.galaxywatch

import android.Manifest
import android.content.pm.ServiceInfo
import android.os.Build
import com.samsung.android.service.health.tracking.data.HealthTrackerType
import com.samsung.android.service.health.tracking.data.PpgType
import com.samsung.android.service.health.tracking.data.ValueKey
import kaist.iclab.tracker.listener.SamsungHealthSensorInitializer
import kaist.iclab.tracker.permission.PermissionManager
import kaist.iclab.tracker.sensor.core.BaseSensor
import kaist.iclab.tracker.sensor.core.SensorConfig
import kaist.iclab.tracker.sensor.core.SensorEntity
import kaist.iclab.tracker.sensor.core.SensorState
import kaist.iclab.tracker.storage.core.StateStorage
import kotlinx.serialization.Serializable

class PPGSensor(
    permissionManager: PermissionManager,
    configStorage: StateStorage<Config>,
    private val stateStorage: StateStorage<SensorState>,
    samsungHealthSensorInitializer: SamsungHealthSensorInitializer
) : BaseSensor<PPGSensor.Config, PPGSensor.Entity>(
    permissionManager, configStorage, stateStorage, Config::class, Entity::class
) {
    override val permissions = listOfNotNull(
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.VANILLA_ICE_CREAM) Manifest.permission.BODY_SENSORS else "com.samsung.android.hardware.sensormanager.permission.READ_ADDITIONAL_HEALTH_DATA",
        // For foreground service type
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
        val green: Int,
        val red: Int,
        val ir: Int,
        val greenStatus: Int,
        val redStatus: Int,
        val irStatus: Int,
    )

    private val tracker by lazy {
        samsungHealthSensorInitializer.getTracker(
            HealthTrackerType.PPG_CONTINUOUS,
            setOf(PpgType.GREEN, PpgType.RED, PpgType.IR)
        )
    }

    private val listener = samsungHealthSensorInitializer.createDataListener { dataPoints ->
        val timestamp = System.currentTimeMillis()
        val entity = Entity(

            dataPoints.map {
                DataPoint(
                    timestamp,
                    it.timestamp,
                    it.getValue(ValueKey.PpgSet.PPG_GREEN),
                    it.getValue(ValueKey.PpgSet.PPG_RED),
                    it.getValue(ValueKey.PpgSet.PPG_IR),
                    it.getValue(ValueKey.PpgSet.GREEN_STATUS),
                    it.getValue(ValueKey.PpgSet.RED_STATUS),
                    it.getValue(ValueKey.PpgSet.IR_STATUS),
                )
            }
        )

        listeners.forEach {
            it.invoke(entity)
        }
    }

    override fun onStart() {
        tracker.setEventListener(listener)
    }

    override fun onStop() {
        tracker.unsetEventListener()
    }
}
