package kaist.iclab.tracker.sensor.galaxywatch

import android.content.Context
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
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
        private const val WINDOW_1M_MS = 60_000L
        private const val WINDOW_5M_MS = 300_000L
        private const val STRIDE_MS = 30_000L

        // HR bounds 30–220 bpm → IBI bounds 60000/220–60000/30 ms.
        private const val MIN_IBI_MS = 60 * 1000 / 220
        private const val MAX_IBI_MS = 60 * 1000 / 30

        private const val MAD_MULTIPLIER = 3.0
        private const val STRESS_PERCENTILE = 0.20

        // RMSSD needs at least one successive-difference pair.
        private const val MIN_IBIS_PER_WINDOW = 2

        // How long to keep valid IBI around purely to anchor the outlier-bound median/MAD -
        // independent of window1mMs/window5mMs, which only bound the RMSSD calculation
        // itself. The intent is "all data collected so far this run"; this cap just keeps a
        // multi-day continuous session from growing the buffer without limit.
        private const val STATS_RETENTION_MS = 24 * 60 * 60 * 1000L

        private const val WAKE_LOCK_TAG = "EnPulse:StressSensorWakeLock"
        // The lock is re-acquired every tick with this multiple of the current stride as
        // its timeout, so it stays continuously held while the loop is healthy but still
        // self-releases within one missed cycle if the loop ever dies without reaching
        // onStop() (mirrors the timeout-bound WakeLock pattern used by
        // DefaultTriggerEngine/AutoSyncManager elsewhere in this codebase).
        private const val WAKE_LOCK_TIMEOUT_MULTIPLIER = 2
    }

    @Serializable
    data class Config(
        val window1mMs: Long = WINDOW_1M_MS,
        val window5mMs: Long = WINDOW_5M_MS,
        val strideMs: Long = STRIDE_MS,
    ) : SensorConfig

    override val initialConfig: Config = Config()

    @Serializable
    data class Entity(
        val received: Long,
        val timestamp: Long,
        // RMSSD over the trailing 1-minute and 5-minute windows. The stress
        // trigger (threshold/isStressed) is decided off rmssd5m only -
        // rmssd1m is reported for visibility/analysis.
        val rmssd1m: Float,
        val ibiCount1m: Int,
        val rmssd5m: Float,
        val ibiCount5m: Int,
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

    private val powerManager by lazy { context.getSystemService(Context.POWER_SERVICE) as PowerManager }
    private var wakeLock: PowerManager.WakeLock? = null

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

        val lock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
        wakeLock = lock

        // Hold the CPU awake for as long as this loop is running rather than relying on
        // AlarmManager wakeups. Neither setRepeating() (batched/deferred under Doze &
        // App Standby) nor setExactAndAllowWhileIdle() (only guarantees a wakeup at the
        // moment it fires, not that the CPU - and the Health Sensor SDK's IBI delivery -
        // stays awake in between; repeat exact-idle alarms are also subject to per-app
        // rate limiting under aggressive OEM battery management, which made things worse
        // on-watch, not better) actually keep the device awake between ticks. A held
        // PARTIAL_WAKE_LOCK does.
        scope.launch {
            // Anchor to an absolute elapsedRealtime() target instead of chaining
            // delay(strideMs) calls back-to-back, so runInference()'s own execution time
            // doesn't accumulate into drift across ticks.
            var nextTickAt = SystemClock.elapsedRealtime() + configStateFlow.value.strideMs
            while (isActive) {
                val strideMs = configStateFlow.value.strideMs
                // Re-acquiring an already-held lock just refreshes its timeout, so this
                // both keeps the CPU awake for the upcoming wait AND guarantees the lock
                // can never leak past 2x strideMs if the loop dies unexpectedly.
                lock.acquire(strideMs * WAKE_LOCK_TIMEOUT_MULTIPLIER)

                val waitMs = nextTickAt - SystemClock.elapsedRealtime()
                if (waitMs > 0) delay(waitMs)

                // The Health Sensor SDK is otherwise free to hold IBI data in its own
                // internal buffer for however long its delivery policy dictates - observed
                // in the field as 100+s stalls followed by an instantaneous catch-up burst
                // of backlogged DataPoints. flush() was previously only requested from
                // onStop(); asking for it every tick here keeps that backlog bounded to
                // roughly one stride instead of growing unboundedly between deliveries.
                // It's a request, not a synchronous read, so the pushed data lands via the
                // normal listener/handleHr path a moment later and is picked up next tick.
                heartRateSensor.flush()

                inferenceMutex.withLock { runInference() }

                // If we still fell behind by more than one stride (e.g. the process was
                // briefly suspended despite the WakeLock), snap forward instead of firing
                // a burst of back-to-back catch-up ticks.
                val now = SystemClock.elapsedRealtime()
                nextTickAt = if (now - nextTickAt > strideMs) now + strideMs else nextTickAt + strideMs
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

        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null

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
                if (n == 0) {
                    Log.d(name, "handleHr: DataPoint@${point.timestamp} carried no IBI values")
                    continue
                }

                // point.timestamp marks when this burst was reported (i.e. the last beat
                // in it), not each individual beat's own time. Reconstruct each beat's
                // timestamp by walking the IBI deltas backward from point.timestamp,
                // instead of stamping every IBI in the burst with the same shared
                // timestamp - that collapsed burst arrival into step changes in
                // ibiCount1m/ibiCount5m independent of the wearer's actual HR stability.
                val beatTimestamps = LongArray(n)
                var t = point.timestamp
                for (i in (n - 1) downTo 0) {
                    beatTimestamps[i] = t
                    t -= if (ibis[i] > 0) ibis[i] else 0
                }

                var accepted = 0
                for (i in 0 until n) {
                    // Galaxy Watch IBI_STATUS: 0 == valid.
                    if (statuses[i] != 0) continue
                    val v = ibis[i]
                    if (v <= 0) continue
                    ibiTimestampsMs.addLast(beatTimestamps[i])
                    ibiValuesMs.addLast(v)
                    accepted++
                }
                if (accepted == 0) {
                    Log.d(name, "handleHr: DataPoint@${point.timestamp} had $n IBI entries, none valid")
                }
            }
        }
    }

    private suspend fun runInference() {
        val config = configStateFlow.value
        val windowEnd = System.currentTimeMillis()
        // The RMSSD windows themselves only need the last 1/5 minutes, but the buffer is
        // kept around much longer (STATS_RETENTION_MS) so outlierBounds() below has "all
        // data collected so far" to anchor its median/MAD on, rather than just whatever a
        // single short window happens to contain.
        val retentionStart = windowEnd - STATS_RETENTION_MS

        val timestamps: LongArray
        val values: IntArray
        synchronized(dataLock) {
            while (ibiTimestampsMs.isNotEmpty() && ibiTimestampsMs.first() < retentionStart) {
                ibiTimestampsMs.removeFirst()
                ibiValuesMs.removeFirst()
            }
            timestamps = ibiTimestampsMs.toLongArray()
            values = ibiValuesMs.toIntArray()
        }

        // Derive the outlier bounds once, from the entire retained history - a single 1 or
        // 5-minute window is too small a sample for its own median/MAD to be a stable
        // outlier reference, and computing it separately per window made them disagree on
        // what counts as an outlier in what is otherwise overlapping underlying data.
        val bounds = outlierBounds(values)
        val raw1m = ibisSince(windowEnd - config.window1mMs, timestamps, values)
        val raw5m = ibisSince(windowEnd - config.window5mMs, timestamps, values)
        val filtered1m = filterIbis(raw1m, bounds)
        val filtered5m = filterIbis(raw5m, bounds)
        Log.d(
            name,
            "runInference: bufferSize=${timestamps.size} ibiCount1m=${filtered1m.size} ibiCount5m=${filtered5m.size}"
        )
        // The 5-minute window drives the stress trigger, so it gates emission.
        if (filtered1m.size < MIN_IBIS_PER_WINDOW || filtered5m.size < MIN_IBIS_PER_WINDOW) {
            Log.w(
                name,
                "runInference: skipping emission, below MIN_IBIS_PER_WINDOW ($MIN_IBIS_PER_WINDOW): " +
                    "ibiCount1m=${filtered1m.size} ibiCount5m=${filtered5m.size}"
            )
            return
        }

        val rmssd1m = rmssd(filtered1m)
        val rmssd5m = rmssd(filtered5m)
        rmssdHistory.insert(windowEnd, rmssd5m)
        val history = rmssdHistory.all()
        val threshold = percentile(history, STRESS_PERCENTILE)

        val emission = Entity(
            received = System.currentTimeMillis(),
            timestamp = windowEnd,
            rmssd1m = rmssd1m,
            ibiCount1m = filtered1m.size,
            rmssd5m = rmssd5m,
            ibiCount5m = filtered5m.size,
            threshold = threshold,
            isStressed = rmssd5m < threshold,
        )
        listeners.forEach { it.invoke(emission) }
    }

    private fun ibisSince(windowStart: Long, timestamps: LongArray, values: IntArray): IntArray {
        val result = ArrayList<Int>(values.size)
        for (i in timestamps.indices) {
            if (timestamps[i] >= windowStart) result.add(values[i])
        }
        return result.toIntArray()
    }

    /**
     * Median +/- [MAD_MULTIPLIER] * MAD computed from [ibis], restricted to physiologically
     * plausible values ([MIN_IBI_MS]..[MAX_IBI_MS]). Callers apply the returned bounds via
     * [filterIbis] - typically to a different (e.g. smaller, overlapping) set of IBIs than
     * the one the bounds were derived from, so the outlier reference stays stable across
     * windows of different sizes drawn from the same underlying data.
     *
     * Returns an empty range (nothing passes) if there's no in-range data to anchor a
     * median on, and an all-inclusive range if the data has no spread (MAD == 0) to derive
     * a meaningful bound from.
     */
    private fun outlierBounds(ibis: IntArray): ClosedFloatingPointRange<Double> {
        val inRange = ibis.filter { it in MIN_IBI_MS..MAX_IBI_MS }.map { it.toDouble() }
        if (inRange.isEmpty()) return Double.POSITIVE_INFINITY..Double.NEGATIVE_INFINITY
        val med = median(inRange)
        val mad = median(inRange.map { abs(it - med) })
        if (mad == 0.0) return Double.NEGATIVE_INFINITY..Double.POSITIVE_INFINITY
        return (med - MAD_MULTIPLIER * mad)..(med + MAD_MULTIPLIER * mad)
    }

    private fun filterIbis(ibis: IntArray, bounds: ClosedFloatingPointRange<Double>): IntArray {
        return ibis.filter { it in MIN_IBI_MS..MAX_IBI_MS && it.toDouble() in bounds }.toIntArray()
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
