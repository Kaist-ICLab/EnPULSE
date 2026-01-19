package kaist.iclab.tracker.sensor.phone

import android.Manifest
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import kaist.iclab.tracker.listener.AlarmListener
import kaist.iclab.tracker.permission.PermissionManager
import kaist.iclab.tracker.sensor.core.BaseSensor
import kaist.iclab.tracker.sensor.core.SensorConfig
import kaist.iclab.tracker.sensor.core.SensorEntity
import kaist.iclab.tracker.sensor.core.SensorState
import kaist.iclab.tracker.storage.core.StateStorage
import kotlinx.serialization.Serializable
import java.util.UUID

class AppUsageLogSensor(
    private val context: Context,
    permissionManager: PermissionManager,
    configStorage: StateStorage<Config>,
    private val stateStorage: StateStorage<SensorState>,
) : BaseSensor<AppUsageLogSensor.Config, AppUsageLogSensor.Entity>(
    permissionManager, configStorage, stateStorage, Config::class, Entity::class
) {
    data class Config(
        val interval: Long,
    ) : SensorConfig

    @Serializable
    data class Entity(
        val eventId: String,
        val received: Long,
        val timestamp: Long,
        val packageName: String,
        val installedBy: String,
        val eventType: Int
    ) : SensorEntity()

    override val permissions = listOfNotNull(Manifest.permission.PACKAGE_USAGE_STATS).toTypedArray()
    override val foregroundServiceTypes: Array<Int> = listOfNotNull(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else null
    ).toTypedArray()

    private val actionName = "kaist.iclab.tracker.${name}_REQUEST"
    private val actionCode = 0x11
    private val alarmListener: AlarmListener = AlarmListener(
        context = context,
        actionName = actionName,
        actionCode = actionCode,
        initialConfig.interval
    )

    // Track last queried timestamp to avoid duplicate events
    private var lastQueriedTimestamp: Long = 0L

    private val mainCallback = { _: Intent? ->
        val usageStatManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val currentTimestamp = System.currentTimeMillis()

        // Query from last queried timestamp (or just the interval for first query)
        val queryStartTime = if (lastQueriedTimestamp > 0) {
            lastQueriedTimestamp + 1  // +1ms to exclude already-queried events
        } else {
            currentTimestamp - configStateFlow.value.interval
        }

        val events = usageStatManager.queryEvents(queryStartTime, currentTimestamp)
        val event = UsageEvents.Event()

        var maxEventTimestamp = lastQueriedTimestamp

        while (events.hasNextEvent()) {
            events.getNextEvent(event)

            // Track the latest event timestamp we've seen
            if (event.timeStamp > maxEventTimestamp) {
                maxEventTimestamp = event.timeStamp
            }

            listeners.forEach { listener ->
                listener.invoke(
                    Entity(
                        UUID.randomUUID().toString(),
                        currentTimestamp,
                        event.timeStamp,
                        event.packageName,
                        isPreinstalledApp(event.packageName),
                        event.eventType
                    )
                )
            }
        }

        // Update last queried timestamp to the latest event we processed
        if (maxEventTimestamp > lastQueriedTimestamp) {
            lastQueriedTimestamp = maxEventTimestamp
        }
    }

    override fun onStart() {
        // Reset tracking on start
        lastQueriedTimestamp = 0L
        alarmListener.addListener(mainCallback)
    }

    override fun onStop() {
        alarmListener.removeListener(mainCallback)
    }

    private fun isPreinstalledApp(packageName: String): String {
        val packageManager = context.packageManager
        try {
            packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA).let {
                return if (it.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                    "SYSTEM"
                } else {
                    "USER"
                }
            }
        } catch (_: PackageManager.NameNotFoundException) {
            return "UNKNOWN"
        }
    }
}