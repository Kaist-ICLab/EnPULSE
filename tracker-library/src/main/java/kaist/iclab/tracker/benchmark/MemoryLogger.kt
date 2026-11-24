package kaist.iclab.tracker.benchmark

import android.content.Context
import android.os.Build
import android.os.Debug
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Benchmarking aid (pradipta/testing only): appends this process's own memory use to a CSV every
 * [DEFAULT_INTERVAL_MS], so EnPULSE's memory can be measured over a run. The benchmark apps can't
 * do this themselves: Android only reports memory details for the caller's own process.
 *
 * Files go to `/sdcard/Android/data/<package>/files/EnPULSE-Memory/`, one per process start. That
 * folder needs no storage permission and adb can read it; `scripts/enpulse_stats.py` pulls and
 * summarises it, and clears it when a run is reset.
 *
 * Cost: one `Debug.getMemoryInfo` call per sample (it reads /proc/self/smaps, typically tens of
 * milliseconds). Samples pause while the CPU sleeps; the timestamps show any gap.
 */
object MemoryLogger {
    private const val TAG = "MemoryLogger"
    const val DIRECTORY = "EnPULSE-Memory"
    private const val DEFAULT_INTERVAL_MS = 60_000L

    private const val HEADER =
        "timestamp_iso,timestamp_ms,total_pss_mb,java_heap_mb,native_heap_mb,code_mb,stack_mb," +
                "graphics_mb,private_other_mb,system_mb,native_heap_allocated_bytes,java_heap_used_bytes\n"

    // Debug.MemoryInfo summary keys, in the same order as the CSV columns after total_pss_mb.
    private val SUMMARY_KEYS = listOf(
        "summary.java-heap",
        "summary.native-heap",
        "summary.code",
        "summary.stack",
        "summary.graphics",
        "summary.private-other",
        "summary.system",
    )

    private var job: Job? = null

    fun start(context: Context, intervalMs: Long = DEFAULT_INTERVAL_MS) {
        if (job?.isActive == true) return
        val dir = context.getExternalFilesDir(DIRECTORY) ?: return
        val startedAt = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        val device = Build.MODEL.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val file = File(dir, "memory_${device}_$startedAt.csv")

        job = CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            while (isActive) {
                try {
                    // Re-checked every time: a reset deletes the folder while the process runs.
                    if (!file.exists()) {
                        dir.mkdirs()
                        file.writeText(HEADER)
                    }
                    file.appendText(sampleRow())
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to log memory sample", e)
                }
                delay(intervalMs)
            }
        }
    }

    private fun sampleRow(): String {
        val info = Debug.MemoryInfo()
        Debug.getMemoryInfo(info)
        val runtime = Runtime.getRuntime()
        val now = System.currentTimeMillis()

        fun mb(kb: Long?) = kb?.let { String.format(Locale.US, "%.1f", it / 1024.0) } ?: ""

        return buildString {
            append(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).format(Date(now))).append(',')
            append(now).append(',')
            append(mb(info.totalPss.toLong())).append(',')
            SUMMARY_KEYS.forEach { key -> append(mb(info.getMemoryStat(key)?.toLongOrNull())).append(',') }
            append(Debug.getNativeHeapAllocatedSize()).append(',')
            append(runtime.totalMemory() - runtime.freeMemory())
            append('\n')
        }
    }
}
