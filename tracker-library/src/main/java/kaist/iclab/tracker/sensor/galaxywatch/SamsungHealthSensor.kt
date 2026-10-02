package kaist.iclab.tracker.sensor.galaxywatch

import android.util.Log
import androidx.compose.ui.graphics.vector.ImageVector
import com.samsung.android.service.health.tracking.HealthTracker
import com.samsung.android.service.health.tracking.data.DataPoint
import com.samsung.android.service.health.tracking.data.HealthTrackerType
import com.samsung.android.service.health.tracking.data.PpgType
import kaist.iclab.tracker.listener.SamsungHealthSensorInitializer
import kaist.iclab.tracker.permission.PermissionManager
import kaist.iclab.tracker.sensor.core.BaseSensor
import kaist.iclab.tracker.sensor.core.SensorConfig
import kaist.iclab.tracker.sensor.core.SensorEntity
import kaist.iclab.tracker.sensor.core.SensorState
import kaist.iclab.tracker.storage.core.StateStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

/**
 * Common base for sensors backed by the Samsung Health Sensor SDK
 * (`com.samsung.android.service.health.tracking`), e.g. sensors running on the
 * Galaxy Watch through [SamsungHealthSensorInitializer].
 *
 * Takes care of acquiring the [HealthTracker], wiring its event listener, and
 * starting/stopping it gracefully on watches where the tracker type isn't
 * supported. Subclasses only need to describe how a single raw [DataPoint] maps
 * to their own data-point type ([mapDataPoint]) and how a batch of those becomes
 * their [SensorEntity] ([toEntity]).
 */
abstract class SamsungHealthSensor<C : SensorConfig, E : SensorEntity, D>(
    permissionManager: PermissionManager,
    configStorage: StateStorage<C>,
    private val stateStorage: StateStorage<SensorState>,
    protected val samsungHealthSensorInitializer: SamsungHealthSensorInitializer,
    configClass: KClass<C>,
    entityClass: KClass<E>,
    titleResId: Int,
    descriptionResId: Int,
    icon: ImageVector,
    private val trackerType: HealthTrackerType,
    private val ppgTypes: Set<PpgType>? = null,
    /**
     * Non-null when this tracker isn't guaranteed to be available on every watch
     * (e.g. only on newer generations). Once the tracking service connects, the
     * sensor is reported as [SensorState.FLAG.UNAVAILABLE] with this message if
     * the tracker turns out to be unsupported.
     */
    private val unavailableMessage: String? = null,
) : BaseSensor<C, E>(
    permissionManager, configStorage, stateStorage, configClass, entityClass,
    titleResId, descriptionResId, icon
) {
    @Volatile
    private var cachedTracker: HealthTracker? = null

    /**
     * The SDK tracker, or null while the tracking service is not connected. Only a successfully
     * obtained tracker is cached: the service connects asynchronously (and reconnects after
     * drops), so a lookup made too early must be retried rather than remembered as null.
     */
    protected val tracker: HealthTracker?
        get() {
            cachedTracker?.let { return it }
            if (!samsungHealthSensorInitializer.connectionStateFlow.value) return null
            return try {
                val ppgTypes = ppgTypes
                val created = if (ppgTypes != null) {
                    samsungHealthSensorInitializer.getTracker(trackerType, ppgTypes)
                } else {
                    samsungHealthSensorInitializer.getTracker(trackerType)
                }
                cachedTracker = created
                created
            } catch (e: Exception) {
                Log.e(name, "Failed to get HealthTracker for $name: ${e.message}")
                null
            }
        }

    private val connectionScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var connectionJob: Job? = null

    /** Maps a single raw SDK data point into this sensor's own data-point type. */
    protected abstract fun mapDataPoint(received: Long, dataPoint: DataPoint): D

    /** Wraps a batch of mapped data points into this sensor's entity. */
    protected abstract fun toEntity(dataPoints: List<D>): E

    protected val listener = samsungHealthSensorInitializer.createDataListener { dataPoints ->
        val received = System.currentTimeMillis()
        val entity = toEntity(dataPoints.map { mapDataPoint(received, it) })
        listeners.forEach { it.invoke(entity) }
    }

    override fun init() {
        super.init()

        val message = unavailableMessage ?: return
        // Since binding to the service takes a while, we subscribe to the connection stateflow
        // and check availability once it is actually bound.
        CoroutineScope(Dispatchers.IO).launch {
            samsungHealthSensorInitializer.connectionStateFlow.collect { isConnected ->
                if (!isConnected) return@collect
                if (!samsungHealthSensorInitializer.isTrackerAvailable(trackerType)) {
                    Log.w(name, "$name is unavailable")
                    stateStorage.set(SensorState(SensorState.FLAG.UNAVAILABLE, message))
                }
                this.cancel()
            }
        }
    }

    override fun onStart() {
        // After a restart or reboot the sensor is started before the tracking service has
        // connected, so register whenever the connection comes (back) up. A StateFlow replays
        // its current value, so an already connected service registers immediately.
        connectionJob?.cancel()
        connectionJob = connectionScope.launch {
            samsungHealthSensorInitializer.connectionStateFlow.collect { isConnected ->
                if (isConnected) {
                    registerListener()
                } else {
                    // A tracker from a dropped connection is stale; fetch a new one on reconnect.
                    cachedTracker = null
                }
            }
        }
    }

    private fun registerListener() {
        try {
            tracker?.setEventListener(listener)
        } catch (_: Exception) {
            // Not supported on this device, ignore
        }
    }

    override fun onStop() {
        connectionJob?.cancel()
        connectionJob = null
        try {
            // Push out whatever is still sitting in the tracker's buffer before we tear
            // down the listener, so the last bit of data isn't lost on stop.
            flush()
            tracker?.unsetEventListener()
        } catch (_: Exception) {
            // Not supported on this device, ignore
        }
    }

    /**
     * Asks the SDK to push any data currently sitting in the tracker's internal buffer
     * right away, instead of waiting for the next scheduled delivery. Buffered data still
     * arrives through the regular [listener]/[mapDataPoint] pipeline; this only requests
     * that it be delivered now.
     *
     * Returns true if the flush request was accepted by the SDK, false if it was rejected
     * (e.g. no listener registered, or this tracker type doesn't support flushing) or if
     * the request failed outright.
     */
    open fun flush(): Boolean {
        val t = tracker ?: return false
        return try {
            t.flush()
        } catch (e: Exception) {
            Log.w(name, "Failed to flush $name: ${e.message}")
            false
        }
    }
}
