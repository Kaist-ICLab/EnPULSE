package kaist.iclab.benchmark

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import java.io.RandomAccessFile

/**
 * Snapshot of device resource metrics at a single point in time.
 */
data class MetricsSnapshot(
    val timestampMs: Long,
    val batteryLevel: Int,          // 0-100 %
    val batteryVoltage: Int,        // mV
    val batteryTemperature: Float,  // °C
    val batteryCurrent: Int,        // mA (negative = discharging)
    val batteryStatus: String,      // "charging" | "discharging" | "full" | "not_charging" | "unknown"
    val cpuUsagePercent: Float,     // 0-100
    val appMemoryMb: Float,        // MB used by this process
    val availableRamMb: Float,     // MB free system RAM
)

/**
 * Collects device resource metrics using only standard Android APIs.
 * No external dependencies required.
 */
class MetricsCollector(private val context: Context) {

    private var prevCpuIdle: Long = 0
    private var prevCpuTotal: Long = 0

    fun collect(): MetricsSnapshot {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val batteryPct = if (scale > 0) (level * 100) / scale else -1

        val voltage = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1
        val tempRaw = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        val temperature = if (tempRaw > 0) tempRaw / 10.0f else -1f

        // Current in microamps, convert to milliamps
        val currentMicroA = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val currentMa = currentMicroA / 1000

        val statusInt = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val statusStr = when (statusInt) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "full"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "not_charging"
            else -> "unknown"
        }

        val cpuUsage = readCpuUsage()

        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        val availableRamMb = memInfo.availMem / (1024f * 1024f)

        val pids = intArrayOf(android.os.Process.myPid())
        val procMemInfo = activityManager.getProcessMemoryInfo(pids)
        val appMemoryMb = if (procMemInfo.isNotEmpty()) {
            procMemInfo[0].totalPss / 1024f // PSS is in KB
        } else 0f

        return MetricsSnapshot(
            timestampMs = System.currentTimeMillis(),
            batteryLevel = batteryPct,
            batteryVoltage = voltage,
            batteryTemperature = temperature,
            batteryCurrent = currentMa,
            batteryStatus = statusStr,
            cpuUsagePercent = cpuUsage,
            appMemoryMb = appMemoryMb,
            availableRamMb = availableRamMb,
        )
    }

    /**
     * Reads overall CPU usage by parsing /proc/stat.
     * Returns percentage as a delta since the last call.
     */
    private fun readCpuUsage(): Float {
        return try {
            val reader = RandomAccessFile("/proc/stat", "r")
            val line = reader.readLine() // "cpu  user nice system idle iowait irq softirq ..."
            reader.close()

            val parts = line.split("\\s+".toRegex())
            if (parts.size < 5) return -1f

            val user = parts[1].toLongOrNull() ?: 0
            val nice = parts[2].toLongOrNull() ?: 0
            val system = parts[3].toLongOrNull() ?: 0
            val idle = parts[4].toLongOrNull() ?: 0
            val iowait = if (parts.size > 5) parts[5].toLongOrNull() ?: 0 else 0
            val irq = if (parts.size > 6) parts[6].toLongOrNull() ?: 0 else 0
            val softirq = if (parts.size > 7) parts[7].toLongOrNull() ?: 0 else 0

            val total = user + nice + system + idle + iowait + irq + softirq
            val idleTime = idle + iowait

            val deltaTotal = total - prevCpuTotal
            val deltaIdle = idleTime - prevCpuIdle

            prevCpuTotal = total
            prevCpuIdle = idleTime

            if (deltaTotal > 0) {
                ((deltaTotal - deltaIdle).toFloat() / deltaTotal.toFloat()) * 100f
            } else {
                0f
            }
        } catch (e: Exception) {
            -1f
        }
    }
}
