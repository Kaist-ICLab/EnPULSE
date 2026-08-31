package kaist.iclab.benchmark

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import java.io.RandomAccessFile

/**
 * Snapshot of device resource metrics at a single point in time.
 *
 * @property timestampMs The time the snapshot was taken, in milliseconds since the epoch.
 * @property batteryLevel Current battery level as a percentage (0-100).
 * @property batteryVoltage Current battery voltage in millivolts (mV).
 * @property batteryTemperature Current battery temperature in degrees Celsius (°C).
 * @property batteryCurrent Current battery current in milliamperes (mA). Negative indicates discharging, positive indicates charging.
 * @property batteryStatus String representation of battery status (e.g., "charging", "discharging", "full", "not_charging", "unknown").
 * @property batteryChargeUah Remaining battery capacity in microampere-hours (µAh).
 * @property batteryEnergyNwh Remaining battery energy in nanowatt-hours (nWh).
 * @property thermalStatus OS-level thermal status code (API 29+), indicating thermal throttling state.
 * @property cpuUsagePercent Overall CPU usage percentage (0-100) since the last snapshot.
 * @property cpuTemperature Best-effort average SoC/CPU temperature in degrees Celsius (°C).
 * @property appMemoryMb Amount of memory (PSS) currently used by this application process in megabytes (MB).
 * @property availableRamMb Amount of free system RAM available in megabytes (MB).
 * @property nativeHeapBytes Amount of native heap memory allocated by this process in bytes.
 */
data class MetricsSnapshot(
    val timestampMs: Long,
    val batteryLevel: Int,
    val batteryVoltage: Int,
    val batteryTemperature: Float,
    val batteryCurrent: Int,
    val batteryStatus: String,
    val batteryChargeUah: Long,
    val batteryEnergyNwh: Long,
    val thermalStatus: Int,
    val cpuUsagePercent: Float,
    val cpuTemperature: Float,
    val appMemoryMb: Float,
    val availableRamMb: Float,
    val nativeHeapBytes: Long,
)

/**
 * Collects device resource metrics using only standard Android APIs.
 * No external dependencies required.
 */
class MetricsCollector(private val context: Context) {

    private var prevCpuIdle: Long = 0
    private var prevCpuTotal: Long = 0

    /**
     * Collects all available device resource metrics using standard Android APIs.
     * This function reads battery state, CPU usage, CPU temperature, and memory usage.
     * 
     * @return A [MetricsSnapshot] object containing the collected metrics.
     */
    fun collect(): MetricsSnapshot {
        val batteryIntent =
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val batteryPct = if (scale > 0) (level * 100) / scale else -1

        val voltage = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1
        val tempRaw = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        val temperature = if (tempRaw > 0) tempRaw / 10.0f else -1f

        // Current in microamps, convert to milliamps
        val currentMicroA =
            batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val currentMa = if (currentMicroA != Int.MIN_VALUE && currentMicroA != 0) {
            currentMicroA / 1000
        } else {
            val avgLink = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)
            if (avgLink != Int.MIN_VALUE && avgLink != 0) {
                avgLink / 1000
            } else {
                -1
            }
        }

        val statusInt = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val statusStr = when (statusInt) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "full"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "not_charging"
            else -> "unknown"
        }

        val chargeUah =
            batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        val energyNwh =
            batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER)

        val powerManager =
            context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        val thermalStatus =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                powerManager.currentThermalStatus
            } else {
                -1
            }

        val cpuUsage = readCpuUsage()
        val cpuTemp = readCpuTemperature()

        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        val availableRamMb = memInfo.availMem / (1024f * 1024f)

        val pids = intArrayOf(android.os.Process.myPid())
        val procMemInfo = activityManager.getProcessMemoryInfo(pids)
        val appMemoryMb = if (procMemInfo.isNotEmpty()) {
            procMemInfo[0].totalPss / 1024f // PSS is in KB
        } else 0f

        val nativeHeapBytes = android.os.Debug.getNativeHeapAllocatedSize()

        return MetricsSnapshot(
            timestampMs = System.currentTimeMillis(),
            batteryLevel = batteryPct,
            batteryVoltage = voltage,
            batteryTemperature = temperature,
            batteryCurrent = currentMa,
            batteryStatus = statusStr,
            batteryChargeUah = chargeUah,
            batteryEnergyNwh = energyNwh,
            thermalStatus = thermalStatus,
            cpuUsagePercent = cpuUsage,
            cpuTemperature = cpuTemp,
            appMemoryMb = appMemoryMb,
            availableRamMb = availableRamMb,
            nativeHeapBytes = nativeHeapBytes,
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

    /**
     * Attempts to read CPU/SoC temperature from sysfs thermal zones.
     * Returns the average temperature in Celsius, or -1f if unsupported/denied.
     */
    private fun readCpuTemperature(): Float {
        try {
            val dir = java.io.File("/sys/class/thermal")
            if (dir.exists() && dir.isDirectory) {
                val zones = dir.listFiles { file -> file.name.startsWith("thermal_zone") }
                if (zones != null) {
                    var sum = 0f
                    var count = 0
                    for (zone in zones) {
                        try {
                            val typeFile = java.io.File(zone, "type")
                            val tempFile = java.io.File(zone, "temp")
                            if (typeFile.exists() && tempFile.exists()) {
                                val type = typeFile.readText().trim().lowercase(java.util.Locale.US)
                                // Look for common CPU/SoC thermal zone names
                                if (type.contains("cpu") || type.contains("soc") || type.contains("tsens") || type.contains(
                                        "mtktscpu"
                                    )
                                ) {
                                    val tempStr = tempFile.readText().trim()
                                    var temp = tempStr.toFloatOrNull() ?: continue
                                    // Some devices report in millidegrees Celsius
                                    if (temp > 1000) {
                                        temp /= 1000f
                                    }
                                    // Ignore clearly invalid temperatures
                                    if (temp in -30f..150f) {
                                        sum += temp
                                        count++
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            // Ignore read errors for individual zones
                        }
                    }
                    if (count > 0) {
                        return sum / count
                    }
                }
            }
        } catch (e: Exception) {
            // Ignored, likely permission denied or path missing
        }
        return -1f
    }
}
