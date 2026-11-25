package kaist.iclab.tracker.benchmark

import android.app.usage.UsageStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Debug
import android.os.PowerManager
import android.os.Process
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Wearable
import kaist.iclab.tracker.sensor.core.Sensor
import kaist.iclab.tracker.sensor.core.SensorState
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit

/** What only the app knows, handed to [BenchmarkRecorder.init]. */
interface BenchmarkAppProbe {
    /**
     * "name" to "value" lines for summary.txt, such as the enabled sensors. Called at run start,
     * on the recorder's own thread.
     */
    fun runInfo(): List<Pair<String, String>> = emptyList()
}

/** Names of the sensors that are enabled or running, for [BenchmarkAppProbe.runInfo]. */
fun enabledSensorNames(sensors: List<Sensor<*, *>>): String =
    sensors.filter {
        val flag = it.sensorStateFlow.value.flag
        flag == SensorState.FLAG.ENABLED || flag == SensorState.FLAG.RUNNING
    }.joinToString { it.name }.ifEmpty { "none" }

/**
 * Process and device readings for each sample, beyond battery and memory. Everything here reads
 * EnPULSE's own process or UID, which Android allows without root or adb.
 *
 * Keeps the previous CPU reading so each row can show the CPU use since the last one.
 */
internal class SystemSampler(private val context: Context) {
    companion object {
        private const val TAG = "SystemSampler"

        val COLUMNS = listOf(
            "cpu_time_ms", "cpu_pct", "threads", "open_fds", "gc_count", "gc_time_ms",
            "app_data_kb", "process_started_ms", "standby_bucket", "screen_on", "device_idle",
            "power_save", "network", "peer_connected",
        )
    }

    private val powerManager = context.getSystemService(PowerManager::class.java)
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)
    private val usageStats = context.getSystemService(UsageStatsManager::class.java)

    // Wall-clock time this process started; the same in every row from one process, so a change
    // between rows marks a restart.
    private val processStartedMs = (System.currentTimeMillis() -
            (SystemClock.elapsedRealtime() - Process.getStartElapsedRealtime())) / 1000 * 1000

    private var prevWallMs: Long? = null
    private var prevCpuMs: Long? = null

    /** Forget the previous readings, so a new run's first row has no deltas. */
    fun reset() {
        prevWallMs = null
        prevCpuMs = null
    }

    /** Values in [COLUMNS] order; an empty string where a value can't be read. */
    fun sample(nowMs: Long): List<String> {
        val cpuMs = Process.getElapsedCpuTime()
        val cpuPct = prevCpuMs?.let { prevCpu ->
            val wall = nowMs - (prevWallMs ?: nowMs)
            if (wall > 0) String.format(Locale.US, "%.2f", (cpuMs - prevCpu) * 100.0 / wall) else null
        }
        prevWallMs = nowMs
        prevCpuMs = cpuMs

        return listOf(
            cpuMs.toString(),
            cpuPct.orEmpty(),
            threadCount()?.toString().orEmpty(),
            File("/proc/self/fd").list()?.size?.toString().orEmpty(),
            Debug.getRuntimeStat("art.gc.gc-count").orEmpty(),
            Debug.getRuntimeStat("art.gc.gc-time").orEmpty(),
            (dirSizeBytes(ContextCompat.getDataDir(context)) / 1024).toString(),
            processStartedMs.toString(),
            standbyBucket(),
            powerManager.isInteractive.toString(),
            powerManager.isDeviceIdleMode.toString(),
            powerManager.isPowerSaveMode.toString(),
            networkType(),
            peerConnected()?.toString().orEmpty(),
        )
    }

    private fun threadCount(): Int? = try {
        File("/proc/self/status").useLines { lines ->
            lines.firstOrNull { it.startsWith("Threads:") }
                ?.substringAfter(':')?.trim()?.toIntOrNull()
        }
    } catch (e: Exception) {
        null
    }

    /** The app's private storage (databases, files, preferences); not the benchmark output. */
    private fun dirSizeBytes(dir: File?): Long =
        dir?.walkTopDown()?.filter { it.isFile }?.sumOf { it.length() } ?: 0L

    private fun standbyBucket(): String = when (val bucket = usageStats.appStandbyBucket) {
        UsageStatsManager.STANDBY_BUCKET_ACTIVE -> "active"
        UsageStatsManager.STANDBY_BUCKET_WORKING_SET -> "working_set"
        UsageStatsManager.STANDBY_BUCKET_FREQUENT -> "frequent"
        UsageStatsManager.STANDBY_BUCKET_RARE -> "rare"
        UsageStatsManager.STANDBY_BUCKET_RESTRICTED -> "restricted"
        5 -> "exempted" // STANDBY_BUCKET_EXEMPTED is a hidden constant
        else -> bucket.toString()
    }

    private fun networkType(): String {
        val caps = connectivity.getNetworkCapabilities(connectivity.activeNetwork) ?: return "none"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> "bluetooth"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ethernet"
            else -> "other"
        }
    }

    /** Whether the paired watch (on the phone) or phone (on the watch) is connected right now. */
    private fun peerConnected(): Boolean? = try {
        Tasks.await(Wearable.getNodeClient(context).connectedNodes, 3, TimeUnit.SECONDS).isNotEmpty()
    } catch (e: Exception) {
        Log.w(TAG, "Could not read connected nodes: ${e.message}")
        null
    }
}
