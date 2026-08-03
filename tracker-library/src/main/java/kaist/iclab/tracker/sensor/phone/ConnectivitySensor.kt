package kaist.iclab.tracker.sensor.phone

import android.Manifest
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import kaist.iclab.tracker.R
import kaist.iclab.tracker.permission.PermissionManager
import kaist.iclab.tracker.sensor.core.BaseSensor
import kaist.iclab.tracker.sensor.core.SensorConfig
import kaist.iclab.tracker.sensor.core.SensorEntity
import kaist.iclab.tracker.sensor.core.SensorState
import kaist.iclab.tracker.storage.core.StateStorage
import kotlinx.serialization.Serializable

class ConnectivitySensor(
    private val context: Context,
    permissionManager: PermissionManager,
    configStorage: StateStorage<Config>,
    stateStorage: StateStorage<SensorState>,
) : BaseSensor<ConnectivitySensor.Config, ConnectivitySensor.Entity>(
    permissionManager, configStorage, stateStorage, Config::class, Entity::class,
    titleResId = R.string.sensor_connectivity,
    descriptionResId = R.string.sensor_desc_connectivity,
    icon = Icons.Default.Wifi
) {
    companion object {
        private const val TAG = "NetworkChangeSensor"
    }

    class Config : SensorConfig

    @Serializable
    data class Entity(
        val received: Long,
        val timestamp: Long,
        val networkType: String,
        val isConnected: Boolean,
        val hasInternet: Boolean,
        val transportTypes: List<String>
    ) : SensorEntity()

    override val permissions = listOfNotNull(
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.INTERNET,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) Manifest.permission.FOREGROUND_SERVICE_SPECIAL_USE else null
    ).toTypedArray()

    override val foregroundServiceTypes: Array<Int> = listOfNotNull(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else null
    ).toTypedArray()

    private val connectivityManager: ConnectivityManager by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    private var isNetworkCallbackRegistered = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            handleNetworkChange(network, "AVAILABLE")
        }

        override fun onLost(network: Network) {
            handleNetworkChange(network, "LOST")
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            handleNetworkChange(network, "CAPABILITIES_CHANGED")
        }
    }

    private fun handleNetworkChange(network: Network, eventType: String) {
        val timestamp = System.currentTimeMillis()
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)

        val networkType = getNetworkType(networkCapabilities)
        val isConnected = networkCapabilities != null
        val hasInternet =
            networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        val transportTypes = getTransportTypes(networkCapabilities)

        listeners.forEach { listener ->
            listener.invoke(
                Entity(
                    received = timestamp,
                    timestamp = timestamp,
                    networkType = networkType,
                    isConnected = isConnected,
                    hasInternet = hasInternet,
                    transportTypes = transportTypes
                )
            )
        }
    }

    private fun getNetworkType(networkCapabilities: NetworkCapabilities?): String {
        if (networkCapabilities == null) return "UNKNOWN"

        return when {
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> "BLUETOOTH"
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            else -> "UNKNOWN"
        }
    }

    private fun getTransportTypes(networkCapabilities: NetworkCapabilities?): List<String> {
        if (networkCapabilities == null) return emptyList()

        val transportTypes = mutableListOf<String>()

        if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            transportTypes.add("WIFI")
        }
        if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            transportTypes.add("CELLULAR")
        }
        if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
            transportTypes.add("ETHERNET")
        }
        if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) {
            transportTypes.add("BLUETOOTH")
        }
        if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
            transportTypes.add("VPN")
        }

        return transportTypes
    }

    override fun onStart() {
        try {
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
            isNetworkCallbackRegistered = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callback", e)
            isNetworkCallbackRegistered = false
        }
    }

    override fun onStop() {
        if (isNetworkCallbackRegistered) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback)
                isNetworkCallbackRegistered = false
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unregister network callback", e)
            }
        }
    }
}
