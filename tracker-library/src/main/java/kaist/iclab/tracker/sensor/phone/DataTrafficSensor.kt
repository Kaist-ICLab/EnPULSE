package kaist.iclab.tracker.sensor.phone

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.TrafficStats
import android.os.Build
import kaist.iclab.tracker.listener.AlarmListener
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DataUsage
import kaist.iclab.tracker.R
import kaist.iclab.tracker.permission.PermissionManager
import kaist.iclab.tracker.sensor.core.BaseSensor
import kaist.iclab.tracker.sensor.core.SensorConfig
import kaist.iclab.tracker.sensor.core.SensorEntity
import kaist.iclab.tracker.sensor.core.SensorState
import kaist.iclab.tracker.storage.core.StateStorage
import kotlinx.serialization.Serializable

class DataTrafficSensor(
    context: Context,
    permissionManager: PermissionManager,
    configStorage: StateStorage<Config>,
    stateStorage: StateStorage<SensorState>,
) : BaseSensor<DataTrafficSensor.Config, DataTrafficSensor.Entity>(
    permissionManager, configStorage, stateStorage, Config::class, Entity::class,
    titleResId = R.string.sensor_data_traffic,
    descriptionResId = R.string.sensor_desc_data_traffic,
    icon = Icons.Default.DataUsage
) {
    data class Config(
        val interval: Long,
    ) : SensorConfig

    @Serializable
    data class Entity(
        val received: Long,
        val timestamp: Long,
        val totalRx: Long,
        val totalTx: Long,
        val mobileRx: Long,
        val mobileTx: Long,
    ) : SensorEntity()

    override val permissions = listOfNotNull(
        Manifest.permission.PACKAGE_USAGE_STATS,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.FOREGROUND_SERVICE,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) Manifest.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE else null,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) Manifest.permission.FOREGROUND_SERVICE_DATA_SYNC else null
    ).toTypedArray()

    override val foregroundServiceTypes: Array<Int> = listOfNotNull(
        ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
    ).toTypedArray()

    private val actionName = "kaist.iclab.tracker.ACTION_DATA_TRAFFIC_STAT"
    private val actionCode = 0x11
    private val alarmListener =
        AlarmListener(context, actionName, actionCode, configStateFlow.value.interval)
    private val mainCallback = { _: Intent? ->
        val timestamp = System.currentTimeMillis()
        listeners.forEach { listener ->
            listener.invoke(
                Entity(
                    timestamp,
                    timestamp,
                    TrafficStats.getTotalRxBytes(),
                    TrafficStats.getTotalTxBytes(),
                    TrafficStats.getMobileRxBytes(),
                    TrafficStats.getMobileTxBytes(),
                )
            )
        }
    }

    override fun onStart() {
        alarmListener.addListener(mainCallback)
    }

    override fun onStop() {
        alarmListener.removeListener(mainCallback)
    }
}