package kaist.iclab.tracker.sensor.phone

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AppRegistration
import kaist.iclab.tracker.R
import kaist.iclab.tracker.permission.PermissionManager
import kaist.iclab.tracker.sensor.core.BaseSensor
import kaist.iclab.tracker.sensor.core.SensorConfig
import kaist.iclab.tracker.sensor.core.SensorEntity
import kaist.iclab.tracker.sensor.core.SensorState
import kaist.iclab.tracker.storage.core.StateStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.milliseconds

class AppListChangeSensor(
    context: Context,
    permissionManager: PermissionManager,
    configStorage: StateStorage<Config>,
    stateStorage: StateStorage<SensorState>,
) : BaseSensor<AppListChangeSensor.Config, AppListChangeSensor.Entity>(
    permissionManager, configStorage, stateStorage, Config::class, Entity::class,
    titleResId = R.string.sensor_app_list_change,
    descriptionResId = R.string.sensor_desc_app_list_change,
    icon = Icons.Default.AppRegistration
) {
    companion object {
        private const val TAG = "AppListChangeSensor"
    }

    data class Config(
        // Interval for periodic updates in milliseconds
        val periodicIntervalMillis: Long,
        // Whether to include system apps in the app list
        val includeSystemApps: Boolean,
        // Whether to include disabled apps in the app list
        val includeDisabledApps: Boolean,
        // Whether to include full app list in entity (can make logs very large)
        val includeFullAppList: Boolean = false,
    ) : SensorConfig

    @Serializable
    data class Entity(
        val received: Long,
        val timestamp: Long,
        val changedApp: AppInfo? = null, // Info about the specific app that changed (if any)
        val appList: List<AppInfo>? = null, // Full snapshot of all apps
    ) : SensorEntity()

    @Serializable
    data class AppInfo(
        val packageName: String,
        val appName: String,
        val versionName: String?,
        val versionCode: Long,
        val installTime: Long,
        val updateTime: Long,
        val isSystemApp: Boolean,
        val isEnabled: Boolean,
        val installerPackage: String?,
    )

    override val permissions = listOfNotNull<String>().toTypedArray()

    override val foregroundServiceTypes: Array<Int> = listOfNotNull(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else null
    ).toTypedArray()

    private val packageManager = context.packageManager

    // Store the last known app list for comparison during app changes
    @Volatile
    private var lastAppList: Set<String> = emptySet()
    private val maxAppListSize = 10000 // Prevent memory issues with too many apps

    // Periodic check mechanism
    private var isMonitoring = false
    private val monitoringScope = CoroutineScope(Dispatchers.IO)

    private fun startPeriodicMonitoring() {
        if (isMonitoring) return
        isMonitoring = true
        monitoringScope.launch {
            while (isMonitoring) {
                try {
                    checkForAppChanges()
                    delay(configStateFlow.value.periodicIntervalMillis.milliseconds)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in periodic monitoring", e)
                    delay(configStateFlow.value.periodicIntervalMillis.milliseconds)
                }
            }
        }
    }

    private fun stopPeriodicMonitoring() {
        isMonitoring = false
        Log.d(TAG, "Stopping periodic app change monitoring")
    }

    private fun checkForAppChanges() {
        val timestamp = System.currentTimeMillis()
        val currentAppList = getInstalledAppPackages()
        val currentAppInfoList = getInstalledAppsInfo()

        // Compare with the previous app list to detect changes
        val addedApps = currentAppList - lastAppList
        val removedApps = lastAppList - currentAppList

        // Only proceed if there are actual changes
        if (addedApps.isNotEmpty() || removedApps.isNotEmpty()) {
            // Determine the changed app (prioritize first added app, then first removed app)
            val changedApp = when {
                addedApps.isNotEmpty() -> {
                    val packageName = addedApps.first()
                    getAppInfo(packageName)
                }

                else -> {
                    val packageName = removedApps.first()
                    AppInfo(
                        packageName = packageName,
                        appName = "Unknown", // App is no longer available
                        versionName = null,
                        versionCode = 0,
                        installTime = 0,
                        updateTime = 0,
                        isSystemApp = false,
                        isEnabled = false,
                        installerPackage = null
                    )
                }
            }

            // Send entity with changed app info
            // Include full app list only if configured to do so
            val shouldIncludeAppList = configStateFlow.value.includeFullAppList
            val combinedEntity = Entity(
                received = timestamp,
                timestamp = timestamp,
                changedApp = changedApp,
                appList = if (shouldIncludeAppList) currentAppInfoList else null
            )
            listeners.forEach { listener ->
                listener.invoke(combinedEntity)
            }
        }

        // Update the stored app list for next comparison
        lastAppList = if (currentAppList.size <= maxAppListSize) {
            currentAppList
        } else {
            Log.w(
                TAG,
                "App list too large (${currentAppList.size}), limiting to $maxAppListSize"
            )
            currentAppList.take(maxAppListSize).toSet()
        }
    }

    private fun getInstalledAppPackages(): Set<String> {
        return try {
            val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
            } else {
                packageManager.getInstalledApplications(0)
            }

            packages.filter { appInfo ->
                val currentConfig = configStateFlow.value
                val includeSystem = currentConfig.includeSystemApps
                val includeDisabled = currentConfig.includeDisabledApps

                val isSystemApp =
                    (appInfo.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0
                val isEnabled = appInfo.enabled

                (includeSystem || !isSystemApp) && (includeDisabled || isEnabled)
            }.map { it.packageName }.toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    private fun getInstalledAppsInfo(): List<AppInfo> {
        return getInstalledAppPackages().mapNotNull { packageName ->
            getAppInfo(packageName)
        }
    }

    private fun getAppInfo(packageName: String): AppInfo? {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                packageManager.getPackageInfo(packageName, 0)
            }

            val applicationInfo = packageInfo.applicationInfo ?: return null
            val appName = packageManager.getApplicationLabel(applicationInfo).toString()

            val isSystemApp =
                (applicationInfo.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0
            val isEnabled = applicationInfo.enabled

            AppInfo(
                packageName = packageName,
                appName = appName,
                versionName = packageInfo.versionName,
                versionCode =
                    packageInfo.longVersionCode,
                installTime = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageInfo.firstInstallTime
                } else {
                    packageInfo.firstInstallTime
                },
                updateTime = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageInfo.lastUpdateTime
                } else {
                    packageInfo.lastUpdateTime
                },
                isSystemApp = isSystemApp,
                isEnabled = isEnabled,
                installerPackage = try {
                    packageManager.getInstallSourceInfo(packageName).installingPackageName
                } catch (_: Exception) {
                    null
                }
            )
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Failed to get app info for $packageName: ${e.message}"
            )
            null
        }
    }

    override fun onStart() {
        // Initialize the last app list for comparison
        lastAppList = getInstalledAppPackages()
        startPeriodicMonitoring()
    }

    override fun onStop() {
        stopPeriodicMonitoring()
    }

}
