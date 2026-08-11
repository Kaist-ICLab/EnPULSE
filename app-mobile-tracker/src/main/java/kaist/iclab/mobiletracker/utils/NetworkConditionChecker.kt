package kaist.iclab.mobiletracker.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kaist.iclab.mobiletracker.Constants

/**
 * Checks the device's current network state against the user's configured auto-sync network
 * preference (any network / WiFi only / mobile only). Shared by [kaist.iclab.mobiletracker.services.upload.SensorAutoSyncWorker]
 * and [kaist.iclab.mobiletracker.services.upload.WebAppLogSyncWorker] — previously duplicated
 * inside the old `AutoSyncService`.
 */
object NetworkConditionChecker {

    fun isMet(context: Context, networkMode: Int): Boolean {
        // If mode is WIFI_MOBILE, any connection is fine
        if (networkMode == Constants.AutoSync.NETWORK_WIFI_MOBILE) {
            return isConnected(context)
        }

        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        val hasWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        val hasCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)

        return when (networkMode) {
            Constants.AutoSync.NETWORK_WIFI_ONLY -> hasWifi
            Constants.AutoSync.NETWORK_MOBILE_ONLY -> hasCellular
            else -> isConnected(context)
        }
    }

    private fun isConnected(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
