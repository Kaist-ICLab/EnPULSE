package kaist.iclab.tracker.sensor.phone

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import kaist.iclab.tracker.listener.BroadcastListener
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiTethering
import kaist.iclab.tracker.R
import kaist.iclab.tracker.permission.PermissionManager
import kaist.iclab.tracker.sensor.core.BaseSensor
import kaist.iclab.tracker.sensor.core.SensorConfig
import kaist.iclab.tracker.sensor.core.SensorEntity
import kaist.iclab.tracker.sensor.core.SensorState
import kaist.iclab.tracker.storage.core.StateStorage
import kotlinx.serialization.Serializable

class WifiScanSensor(
    private val context: Context,
    permissionManager: PermissionManager,
    configStorage: StateStorage<Config>,
    private val stateStorage: StateStorage<SensorState>,
) : BaseSensor<WifiScanSensor.Config, WifiScanSensor.Entity>(
    permissionManager, configStorage, stateStorage, Config::class, Entity::class,
    titleResId = R.string.sensor_wifi_scan,
    descriptionResId = R.string.sensor_desc_wifi,
    icon = Icons.Default.WifiTethering
) {
    class Config : SensorConfig

    @Serializable
    data class Entity(
        val received: Long,
        val timestamp: Long,
        val ssid: String,
        val bssid: String,
        val frequency: Int,
        val level: Int
    ) : SensorEntity()

    override val permissions = listOfNotNull(
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) Manifest.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE else null,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) Manifest.permission.FOREGROUND_SERVICE_LOCATION else null,
    ).toTypedArray()

    override val foregroundServiceTypes: Array<Int> = (
            listOfNotNull(
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
            ).toTypedArray()

    private val wifiManager: WifiManager by lazy {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    private val broadcastListener = BroadcastListener(
        context,
        arrayOf(
            WifiManager.SCAN_RESULTS_AVAILABLE_ACTION
        )
    )

    private val mainCallback = mainCallback@{ intent: Intent? ->
        if (intent == null) return@mainCallback

        try {
            val results = wifiManager.scanResults
            val timestamp = System.currentTimeMillis()

            results.forEach { result ->
                listeners.forEach { listener ->
                    listener.invoke(
                        Entity(
                            timestamp,
                            timestamp,
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                result.wifiSsid?.toString() ?: "UNKNOWN"
                            } else {
                                @Suppress("DEPRECATION")
                                result.SSID
                            },
                            result.BSSID,
                            result.frequency,
                            result.level,
                        )
                    )
                }
            }

        } catch (e: SecurityException) {
            throw e
        }
    }

    override fun init() {
        super.init()
        // TODO: Wi-fi status can change any time?
        if (!wifiManager.isWifiEnabled) {
            stateStorage.set(SensorState(SensorState.FLAG.UNAVAILABLE, "WiFi is disabled"))
        }
    }

    override fun onStart() {
        broadcastListener.addListener(mainCallback)
    }

    override fun onStop() {
        broadcastListener.removeListener(mainCallback)
    }
}