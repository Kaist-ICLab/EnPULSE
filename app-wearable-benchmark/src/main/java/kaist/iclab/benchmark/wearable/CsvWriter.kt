package kaist.iclab.benchmark.wearable

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Writes metrics snapshots to a CSV file in the app's external files directory.
 * On Wear OS, MediaStore is not reliable, so we use getExternalFilesDir("Benchmarks").
 * Files can be pulled via: adb pull /sdcard/Android/data/kaist.iclab.benchmark.wearable/files/Benchmarks ./
 * Thread-safe — all writes are synchronized.
 */
class CsvWriter(private val context: Context) {

    companion object {
        private const val CSV_HEADER =
            "timestamp_iso,timestamp_ms,battery_level_pct,battery_voltage_mv," +
                    "battery_temp_c,battery_current_ma,battery_status," +
                    "battery_charge_uah,battery_energy_nwh,thermal_status," +
                    "cpu_usage_pct,cpu_temperature_c,app_memory_mb,available_ram_mb,native_heap_bytes\n"

        private val ISO_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US)
        private val FILE_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
    }

    private var outputStream: OutputStream? = null
    private var folderPath: String? = null
    private var folder: File? = null
    private val lock = Any()

    /**
     * Opens a new CSV file for writing in a unique folder.
     * @param scenarioName Label for this benchmark run.
     * @return The folder absolute path created.
     */
    fun open(scenarioName: String): String {
        synchronized(lock) {
            close() // Close any previously open file

            val sanitizedName = scenarioName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val timestamp = FILE_DATE_FORMAT.format(Date())
            val deviceModel = android.os.Build.MODEL.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val folderName = "watch-${deviceModel}_${sanitizedName}_$timestamp"

            val baseDir = context.getExternalFilesDir("Benchmarks")
                ?: throw IllegalStateException("External files directory is not available")

            val dir = File(baseDir, folderName)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            folder = dir
            folderPath = dir.absolutePath

            val csvFile = File(dir, "metrics.csv")
            outputStream = FileOutputStream(csvFile, true)

            // Write header
            outputStream!!.write(CSV_HEADER.toByteArray())
            outputStream!!.flush()

            return dir.absolutePath
        }
    }

    /**
     * Appends a single metrics snapshot as a CSV row.
     */
    fun write(snapshot: MetricsSnapshot) {
        synchronized(lock) {
            val stream = outputStream ?: return

            val isoTime = ISO_FORMAT.format(Date(snapshot.timestampMs))
            val row = buildString {
                append(isoTime).append(',')
                append(snapshot.timestampMs).append(',')
                append(snapshot.batteryLevel).append(',')
                append(snapshot.batteryVoltage).append(',')
                append(String.format(Locale.US, "%.1f", snapshot.batteryTemperature)).append(',')
                append(snapshot.batteryCurrent).append(',')
                append(snapshot.batteryStatus).append(',')
                append(snapshot.batteryChargeUah).append(',')
                append(snapshot.batteryEnergyNwh).append(',')
                append(snapshot.thermalStatus).append(',')
                append(String.format(Locale.US, "%.1f", snapshot.cpuUsagePercent)).append(',')
                append(String.format(Locale.US, "%.1f", snapshot.cpuTemperature)).append(',')
                append(String.format(Locale.US, "%.1f", snapshot.appMemoryMb)).append(',')
                append(String.format(Locale.US, "%.1f", snapshot.availableRamMb)).append(',')
                append(snapshot.nativeHeapBytes)
                append('\n')
            }

            stream.write(row.toByteArray())
            stream.flush()
        }
    }

    /**
     * Writes a summary text file to the same folder when the benchmark ends.
     */
    fun writeSummary(summaryText: String) {
        synchronized(lock) {
            val dir = folder ?: return
            try {
                val summaryFile = File(dir, "summary.txt")
                summaryFile.writeText(summaryText)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Returns the current folder path, or null if no file is open.
     */
    fun getFolderPath(): String? = synchronized(lock) { folderPath }

    /**
     * Closes the CSV file.
     */
    fun close() {
        synchronized(lock) {
            try {
                outputStream?.close()
            } catch (_: Exception) {
                // Ignore close errors
            }
            outputStream = null
        }
    }
}
