package kaist.iclab.tracker.sensor.galaxywatch

import android.Manifest
import android.content.pm.ServiceInfo
import android.os.Build
import com.samsung.android.service.health.tracking.data.HealthTrackerType
import com.samsung.android.service.health.tracking.data.PpgType
import com.samsung.android.service.health.tracking.data.ValueKey
import com.samsung.android.service.health.tracking.data.DataPoint as HealthDataPoint
import kaist.iclab.tracker.listener.SamsungHealthSensorInitializer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonitorHeart
import kaist.iclab.tracker.R
import kaist.iclab.tracker.permission.PermissionManager
import kaist.iclab.tracker.sensor.core.SensorConfig
import kaist.iclab.tracker.sensor.core.SensorEntity
import kaist.iclab.tracker.sensor.core.SensorState
import kaist.iclab.tracker.storage.core.StateStorage
import kotlinx.serialization.Serializable

class PPGSensor(
    permissionManager: PermissionManager,
    configStorage: StateStorage<Config>,
    stateStorage: StateStorage<SensorState>,
    samsungHealthSensorInitializer: SamsungHealthSensorInitializer
) : SamsungHealthSensor<PPGSensor.Config, PPGSensor.Entity, PPGSensor.DataPoint>(
    permissionManager, configStorage, stateStorage, samsungHealthSensorInitializer,
    Config::class, Entity::class,
    titleResId = R.string.sensor_ppg,
    descriptionResId = R.string.sensor_desc_ppg,
    icon = Icons.Default.MonitorHeart,
    trackerType = HealthTrackerType.PPG_CONTINUOUS,
    ppgTypes = setOf(PpgType.GREEN, PpgType.RED, PpgType.IR)
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
        val green: Int,
        val red: Int,
        val ir: Int,
        val greenStatus: Int,
        val redStatus: Int,
        val irStatus: Int,
    )

    override fun mapDataPoint(received: Long, dataPoint: HealthDataPoint): DataPoint =
        DataPoint(
            received,
            dataPoint.timestamp,
            dataPoint.getValue(ValueKey.PpgSet.PPG_GREEN),
            dataPoint.getValue(ValueKey.PpgSet.PPG_RED),
            dataPoint.getValue(ValueKey.PpgSet.PPG_IR),
            dataPoint.getValue(ValueKey.PpgSet.GREEN_STATUS),
            dataPoint.getValue(ValueKey.PpgSet.RED_STATUS),
            dataPoint.getValue(ValueKey.PpgSet.IR_STATUS),
        )

    override fun toEntity(dataPoints: List<DataPoint>): Entity = Entity(dataPoints)
}
