package kaist.iclab.tracker.sensor.galaxywatch

import android.Manifest
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import com.samsung.android.service.health.tracking.data.HealthTrackerType
import com.samsung.android.service.health.tracking.data.ValueKey
import com.samsung.android.service.health.tracking.data.DataPoint as HealthDataPoint
import kaist.iclab.tracker.listener.SamsungHealthSensorInitializer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import kaist.iclab.tracker.R
import kaist.iclab.tracker.permission.PermissionManager
import kaist.iclab.tracker.sensor.core.SensorConfig
import kaist.iclab.tracker.sensor.core.SensorEntity
import kaist.iclab.tracker.sensor.core.SensorState
import kaist.iclab.tracker.storage.core.StateStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

// NOTE: ECG measurement is a regulated feature and is only available on certain
// Galaxy Watch models/regions where Samsung Health's ECG app has been approved.

class ECGSensor(
    permissionManager: PermissionManager,
    configStorage: StateStorage<Config>,
    stateStorage: StateStorage<SensorState>,
    samsungHealthSensorInitializer: SamsungHealthSensorInitializer
) : SamsungHealthSensor<ECGSensor.Config, ECGSensor.Entity, ECGSensor.DataPoint>(
    permissionManager, configStorage, stateStorage, samsungHealthSensorInitializer,
    Config::class, Entity::class,
    titleResId = R.string.sensor_ecg,
    descriptionResId = R.string.sensor_desc_ecg,
    icon = Icons.Default.Favorite,
    trackerType = HealthTrackerType.ECG_ON_DEMAND,
    unavailableMessage = "ECG not supported on this device"
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
    @Serializable
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
        val ecgMv: Float,
        val leadOff: Int,
        val sequence: Int,
        val ppgGreen: Int,
        val maxThresholdMv: Float,
        val minThresholdMv: Float,
    )

    override fun mapDataPoint(received: Long, dataPoint: HealthDataPoint): DataPoint =
        DataPoint(
            received,
            dataPoint.timestamp,
            dataPoint.getValue(ValueKey.EcgSet.ECG_MV),
            dataPoint.getValue(ValueKey.EcgSet.LEAD_OFF),
            dataPoint.getValue(ValueKey.EcgSet.SEQUENCE),
            dataPoint.getValue(ValueKey.EcgSet.PPG_GREEN),
            dataPoint.getValue(ValueKey.EcgSet.MAX_THRESHOLD_MV),
            dataPoint.getValue(ValueKey.EcgSet.MIN_THRESHOLD_MV),
        )

    override fun toEntity(dataPoints: List<DataPoint>): Entity = Entity(dataPoints)

    // ECG is on-demand: the tracker can only be armed once the tracking service is actually
    // connected, so we wait for connectionStateFlow instead of arming it eagerly.
    private var connectionJob: kotlinx.coroutines.Job? = null

    override fun onStart() {
        connectionJob?.cancel()
        connectionJob = CoroutineScope(Dispatchers.IO).launch {
            samsungHealthSensorInitializer.connectionStateFlow.collect { isConnected ->
                if (isConnected) {
                    try {
                        tracker?.setEventListener(listener)
                    } catch (e: Exception) {
                        Log.w(name, "Failed to start ECG sensor: ${e.message}")
                    }
                    this.cancel()
                }
            }
        }
    }

    override fun onStop() {
        connectionJob?.cancel()
        connectionJob = null
        try {
            if (samsungHealthSensorInitializer.connectionStateFlow.value) {
                // Push out whatever is still sitting in the tracker's buffer before we tear
                // down the listener, so the last bit of data isn't lost on stop.
                flush()
                tracker?.unsetEventListener()
            }
        } catch (e: Exception) {
            Log.w(name, "Failed to stop ECG sensor: ${e.message}")
        }
    }
}
