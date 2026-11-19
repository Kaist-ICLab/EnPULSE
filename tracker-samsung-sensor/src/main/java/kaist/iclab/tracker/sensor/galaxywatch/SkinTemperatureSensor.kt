package kaist.iclab.tracker.sensor.galaxywatch

import android.Manifest
import android.content.pm.ServiceInfo
import android.health.connect.HealthPermissions
import android.os.Build
import com.samsung.android.service.health.tracking.data.HealthTrackerType
import com.samsung.android.service.health.tracking.data.ValueKey
import com.samsung.android.service.health.tracking.data.DataPoint as HealthDataPoint
import kaist.iclab.tracker.listener.SamsungHealthSensorInitializer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Thermostat
import kaist.iclab.tracker.R
import kaist.iclab.tracker.permission.PermissionManager
import kaist.iclab.tracker.sensor.core.SensorConfig
import kaist.iclab.tracker.sensor.core.SensorEntity
import kaist.iclab.tracker.sensor.core.SensorState
import kaist.iclab.tracker.storage.core.StateStorage
import kotlinx.serialization.Serializable

// NOTE: Skin Temperature measurement is available with Galaxy Watch 5 series or later models.

class SkinTemperatureSensor(
    permissionManager: PermissionManager,
    configStorage: StateStorage<Config>,
    stateStorage: StateStorage<SensorState>,
    samsungHealthSensorInitializer: SamsungHealthSensorInitializer
) : SamsungHealthSensor<SkinTemperatureSensor.Config, SkinTemperatureSensor.Entity, SkinTemperatureSensor.DataPoint>(
    permissionManager, configStorage, stateStorage, samsungHealthSensorInitializer,
    Config::class, Entity::class,
    titleResId = R.string.sensor_skin_temperature,
    descriptionResId = R.string.sensor_desc_skin_temperature,
    icon = Icons.Default.Thermostat,
    trackerType = HealthTrackerType.SKIN_TEMPERATURE_CONTINUOUS
) {
    override val id: String = "SkinTemperature"
    override val permissions = listOfNotNull(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) HealthPermissions.READ_SKIN_TEMPERATURE else Manifest.permission.BODY_SENSORS,
        // For foreground service type
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && Build.VERSION.SDK_INT <= Build.VERSION_CODES.VANILLA_ICE_CREAM) Manifest.permission.BODY_SENSORS_BACKGROUND else null,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND else null
    ).toTypedArray()

    override val foregroundServiceTypes: Array<Int> = listOfNotNull(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH else null
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
        val objectTemperature: Float,
        val ambientTemperature: Float,
        val status: Int
    )

    override fun mapDataPoint(received: Long, dataPoint: HealthDataPoint): DataPoint =
        DataPoint(
            received,
            dataPoint.timestamp,
            dataPoint.getValue(ValueKey.SkinTemperatureSet.OBJECT_TEMPERATURE) ?: 0.0f,
            dataPoint.getValue(ValueKey.SkinTemperatureSet.AMBIENT_TEMPERATURE) ?: 0.0f,
            dataPoint.getValue(ValueKey.SkinTemperatureSet.STATUS) ?: 0
        )

    override fun toEntity(dataPoints: List<DataPoint>): Entity = Entity(dataPoints)
}
