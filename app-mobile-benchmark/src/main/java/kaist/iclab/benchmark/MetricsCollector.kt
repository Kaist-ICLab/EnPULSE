package kaist.iclab.benchmark

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

/**
 * Snapshot of device resource metrics at a single point in time.
 *
 * @property timestampMs The time the snapshot was taken, in milliseconds since the epoch.
 * @property batteryLevel Current battery level as a percentage (0-100).
 * @property batteryVoltage Current battery voltage in millivolts (mV).
 * @property batteryTemperature Current battery temperature in degrees Celsius (°C).
 * @property batteryStatus String representation of battery status (e.g., "charging", "discharging", "full", "not_charging", "unknown").
 * @property batteryChargeUah Remaining battery capacity in microampere-hours (µAh).
 * @property batteryEnergyNwh Remaining battery energy in nanowatt-hours (nWh).
 * @property appMemoryMb Amount of memory (PSS) currently used by this application process in megabytes (MB).
 * @property availableRamMb Amount of free system RAM available in megabytes (MB).
 * @property nativeHeapBytes Amount of native heap memory allocated by this process in bytes.
 */
data class MetricsSnapshot(
    val timestampMs: Long,
    val batteryLevel: Int,
    val batteryVoltage: Int,
    val batteryTemperature: Float,
    val batteryStatus: String,
    val batteryChargeUah: Long,
    val batteryEnergyNwh: Long,
    val appMemoryMb: Float,
    val availableRamMb: Float,
    val nativeHeapBytes: Long,
)

/**
 * Collects device resource metrics using only standard Android APIs.
 * No external dependencies required.
 */
class MetricsCollector(private val context: Context) {

    /**
     * Collects all available device resource metrics using standard Android APIs.
     * This function reads battery state and memory usage.
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
            batteryStatus = statusStr,
            batteryChargeUah = chargeUah,
            batteryEnergyNwh = energyNwh,
            appMemoryMb = appMemoryMb,
            availableRamMb = availableRamMb,
            nativeHeapBytes = nativeHeapBytes,
        )
    }
}
