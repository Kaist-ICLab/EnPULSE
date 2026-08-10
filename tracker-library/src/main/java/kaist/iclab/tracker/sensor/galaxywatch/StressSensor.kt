package kaist.iclab.tracker.sensor.galaxywatch

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import kaist.iclab.tracker.R
import kaist.iclab.tracker.permission.PermissionManager
import kaist.iclab.tracker.sensor.core.BaseSensor
import kaist.iclab.tracker.sensor.core.SensorConfig
import kaist.iclab.tracker.sensor.core.SensorEntity
import kaist.iclab.tracker.sensor.core.SensorState
import kaist.iclab.tracker.storage.core.RmssdHistory
import kaist.iclab.tracker.storage.core.StateStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.milliseconds

class StressSensor(
    private val context: Context,
    permissionManager: PermissionManager,
    configStorage: StateStorage<Config>,
    stateStorage: StateStorage<SensorState>,
    private val heartRateSensor: HeartRateSensor,
    private val rmssdHistory: RmssdHistory,
) : BaseSensor<StressSensor.Config, StressSensor.Entity>(
    permissionManager, configStorage, stateStorage, Config::class, Entity::class,
    titleResId = R.string.sensor_stress,
    descriptionResId = R.string.sensor_desc_stress,
    icon = Icons.Default.Psychology
) {
    companion object {
        private const val WINDOW_MS = 60_000L
        private const val STRIDE_MS = 15_000L

        // HR bounds 30–220 bpm → IBI bounds 60000/220–60000/30 ms.
        private const val MIN_IBI_MS = 60 * 1000 / 220
        private const val MAX_IBI_MS = 60 * 1000 / 30

        private const val MAD_MULTIPLIER = 3.0
        private const val STRESS_PERCENTILE = 0.20

        // RMSSD needs at least one successive-difference pair.
        private const val MIN_IBIS_PER_WINDOW = 2
    }

    @Serializable
    data class Config(
        val windowMs: Long = WINDOW_MS,
        val strideMs: Long = STRIDE_MS,
    ) : SensorConfig

    override val initialConfig: Config = Config()

    @Serializable
    data class Entity(
        val received: Long,
        val timestamp: Long,
        val rmssd: Float,
        val ibiCount: Int,
        val threshold: Float,
        val isStressed: Boolean,
    ) : SensorEntity()

    override val id: String = "Stress"
    override val permissions: Array<String> get() = heartRateSensor.permissions
    override val foregroundServiceTypes: Array<Int> get() = heartRateSensor.foregroundServiceTypes

    private val dataLock = Any()
    private val ibiTimestampsMs = ArrayDeque<Long>()
    private val ibiValuesMs = ArrayDeque<Int>()

    private val inferenceMutex = Mutex()
    private var inferenceScope: CoroutineScope? = null

    private var ownsHr = false
    private val hrListener: (HeartRateSensor.Entity) -> Unit = { handleHr(it) }

    override fun init() {
        super.init()
        heartRateSensor.init()
    }

    override fun onStart() {
        synchronized(dataLock) {
            ibiTimestampsMs.clear()
            ibiValuesMs.clear()
        }
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        inferenceScope = scope

        heartRateSensor.addListener(hrListener)
        ownsHr = ensureRunning(heartRateSensor)

        // Drive inference off a self-rescheduling coroutine instead of a repeating
        // AlarmManager alarm. AlarmManager treats setRepeating() as inexact and batches/
        // defers it under Doze & App Standby - at a 15s stride that meant the actual fire
        // times (and therefore the window each inference saw, and its ibiCount) drifted
        // unpredictably instead of landing every strideMs.
        val strideMs = configStateFlow.value.strideMs
        scope.launch {
            while (isActive) {
                delay(strideMs.milliseconds)
                inferenceMutex.withLock { runInference() }
            }
        }
    }

    override fun onStop() {
        heartRateSensor.removeListener(hrListener)
        if (ownsHr && heartRateSensor.sensorStateFlow.value.flag == SensorState.FLAG.RUNNING) {
            heartRateSensor.stop()
        }
        ownsHr = false

        inferenceScope?.cancel()
        inferenceScope = null

        synchronized(dataLock) {
            ibiTimestampsMs.clear()
            ibiValuesMs.clear()
        }
    }

    private fun ensureRunning(sensor: BaseSensor<*, *>): Boolean {
        if (sensor.sensorStateFlow.value.flag == SensorState.FLAG.DISABLED) {
            sensor.enable()
        }
        return if (sensor.sensorStateFlow.value.flag != SensorState.FLAG.RUNNING) {
            sensor.start()
            true
        } else {
            false
        }
    }

    private fun handleHr(entity: HeartRateSensor.Entity) {
        synchronized(dataLock) {
            for (point in entity.dataPoint) {
                val ibis = point.ibi
                val statuses = point.ibiStatus
                val n = minOf(ibis.size, statuses.size)
                for (i in 0 until n) {
                    // Galaxy Watch IBI_STATUS: 0 == valid.
                    if (statuses[i] != 0) continue
                    val v = ibis[i]
                    if (v <= 0) continue
                    ibiTimestampsMs.addLast(point.timestamp)
                    ibiValuesMs.addLast(v)
                }
            }
        }
    }

    private suspend fun runInference() {
        val config = configStateFlow.value
        val windowEnd = System.currentTimeMillis()
        val windowStart = windowEnd - config.windowMs

        val ibis: IntArray = synchronized(dataLock) {
            while (ibiTimestampsMs.isNotEmpty() && ibiTimestampsMs.first() < windowStart) {
                ibiTimestampsMs.removeFirst()
                ibiValuesMs.removeFirst()
            }
            ibiValuesMs.toIntArray()
        }

        val filtered = filterIbis(ibis)
        if (filtered.size < MIN_IBIS_PER_WINDOW) return

        val rmssd = rmssd(filtered)
        rmssdHistory.insert(windowEnd, rmssd)
        val history = rmssdHistory.all()
        val threshold = percentile(history, STRESS_PERCENTILE)

        val emission = Entity(
            received = System.currentTimeMillis(),
            timestamp = windowEnd,
            rmssd = rmssd,
            ibiCount = filtered.size,
            threshold = threshold,
            isStressed = rmssd < threshold,
        )
        listeners.forEach { it.invoke(emission) }
    }

    private fun filterIbis(ibis: IntArray): IntArray {
        val inRange = ibis.filter { it in MIN_IBI_MS..MAX_IBI_MS }.map { it.toDouble() }
        if (inRange.isEmpty()) return IntArray(0)
        val med = median(inRange)
        val mad = median(inRange.map { abs(it - med) })
        if (mad == 0.0) return inRange.map { it.toInt() }.toIntArray()
        val low = med - MAD_MULTIPLIER * mad
        val high = med + MAD_MULTIPLIER * mad
        return inRange.filter { it in low..high }.map { it.toInt() }.toIntArray()
    }

    private fun rmssd(ibis: IntArray): Float {
        var sumSq = 0.0
        for (i in 1 until ibis.size) {
            val d = (ibis[i] - ibis[i - 1]).toDouble()
            sumSq += d * d
        }
        return sqrt(sumSq / (ibis.size - 1)).toFloat()
    }

    private fun median(values: List<Double>): Double {
        val sorted = values.sorted()
        val n = sorted.size
        return if (n % 2 == 1) sorted[n / 2]
        else (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0
    }

    private fun percentile(values: FloatArray, p: Double): Float {
        if (values.isEmpty()) return Float.NaN
        val sorted = values.copyOf().also { it.sort() }
        if (sorted.size == 1) return sorted[0]
        val rank = p * (sorted.size - 1)
        val lo = rank.toInt()
        val hi = (lo + 1).coerceAtMost(sorted.size - 1)
        val frac = rank - lo
        return (sorted[lo] * (1 - frac) + sorted[hi] * frac).toFloat()
    }
}
