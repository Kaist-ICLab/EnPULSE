package kaist.iclab.tracker.sensor.galaxywatch

import android.Manifest
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import com.samsung.android.service.health.tracking.data.HealthTrackerType
import com.samsung.android.service.health.tracking.data.ValueKey
import kaist.iclab.tracker.listener.SamsungHealthSensorInitializer
import kaist.iclab.tracker.permission.PermissionManager
import kaist.iclab.tracker.sensor.core.BaseSensor
import kaist.iclab.tracker.sensor.core.SensorConfig
import kaist.iclab.tracker.sensor.core.SensorEntity
import kaist.iclab.tracker.sensor.core.SensorState
import kaist.iclab.tracker.storage.core.StateStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

// NOTE: EDA measurement is available with Galaxy Watch 8 series or later models.

class EDASensor(
    permissionManager: PermissionManager,
    configStorage: StateStorage<Config>,
    private val stateStorage: StateStorage<SensorState>,
    private val samsungHealthSensorInitializer: SamsungHealthSensorInitializer,
) : BaseSensor<EDASensor.Config, EDASensor.Entity>(
    permissionManager, configStorage, stateStorage, Config::class, Entity::class
) {
    override val permissions = listOfNotNull(
        // For foreground service type
        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.VANILLA_ICE_CREAM) Manifest.permission.BODY_SENSORS else "com.samsung.android.hardware.sensormanager.permission.READ_ADDITIONAL_HEALTH_DATA",
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && Build.VERSION.SDK_INT <= Build.VERSION_CODES.VANILLA_ICE_CREAM) Manifest.permission.BODY_SENSORS_BACKGROUND else null,
//        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND else null
    ).toTypedArray()

    override val foregroundServiceTypes: Array<Int> = listOfNotNull(
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && Build.VERSION.SDK_INT <= Build.VERSION_CODES.VANILLA_ICE_CREAM) ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH else null
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
        val skinConductance: Float,
        val status: Int
    )

    private val tracker by lazy {
        samsungHealthSensorInitializer.getTracker(HealthTrackerType.EDA_CONTINUOUS)
    }

    private val listener = samsungHealthSensorInitializer.createDataListener { dataPoints ->
        val timestamp = System.currentTimeMillis()
        val entity = Entity(
            dataPoints.map {
                DataPoint(
                    timestamp,
                    it.timestamp,
                    it.getValue(ValueKey.EdaSet.SKIN_CONDUCTANCE),
                    it.getValue(ValueKey.EdaSet.STATUS)
                )
            }
        )

        listeners.forEach {
            it.invoke(entity)
        }
    }

    override fun init() {
        super.init()

        // Check EDA support on this device
        // Since binding to the service takes a while, we subscribe to the connection stateflow and check it when it is actually binded
        CoroutineScope(Dispatchers.IO).launch {
            samsungHealthSensorInitializer.connectionStateFlow.collect { isConnected ->
                if (!isConnected) return@collect
                if (!samsungHealthSensorInitializer.isTrackerAvailable(HealthTrackerType.EDA_CONTINUOUS)) {
                    Log.w(name, "EDASensor is unavailable")
                    stateStorage.set(
                        SensorState(
                            SensorState.FLAG.UNAVAILABLE,
                            "EDA not supported on this device"
                        )
                    )
                }

                this.cancel()
            }
        }
    }

    override fun onStart() {
        try {
            tracker.setEventListener(listener)
        } catch (e: UnsupportedOperationException) {
            // EDA not supported on this device, ignore
        }
    }

    override fun onStop() {
        try {
            tracker.unsetEventListener()
        } catch (e: UnsupportedOperationException) {
            // EDA not supported on this device, ignore
        }
    }
}
