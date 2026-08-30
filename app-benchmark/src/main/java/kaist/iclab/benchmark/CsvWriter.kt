package kaist.iclab.benchmark

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Writes metrics snapshots to a CSV file in Downloads/EnPULSE/Benchmark_<Scenario>_<Date>/.
 * Uses MediaStore API for scoped storage compatibility (Android 10+).
 * Thread-safe — all writes are synchronized.
 */
class CsvWriter(private val context: Context) {

    companion object {
        private const val BASE_DIRECTORY = "EnPULSE"

        private const val CSV_HEADER =
            "timestamp_iso,timestamp_ms,battery_level_pct,battery_voltage_mv," +
            "battery_temp_c,battery_current_ma,battery_status," +
            "cpu_usage_pct,app_memory_mb,available_ram_mb\n"

        private val ISO_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US)
        private val FILE_DATE_FORMAT = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
    }

    private var outputStream: OutputStream? = null
    private var fileName: String? = null
    private var folderPath: String? = null
    private val lock = Any()

    /**
     * Opens a new CSV file for writing in a unique folder.
     * @param scenarioName Label for this benchmark run.
     * @return The folder relative path created.
     */
    fun open(scenarioName: String): String {
        synchronized(lock) {
            close() // Close any previously open file

            val sanitizedName = scenarioName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val timestamp = FILE_DATE_FORMAT.format(Date())
            val folderName = "Benchmark_${sanitizedName}_$timestamp"
            
            val relativeDir = "${Environment.DIRECTORY_DOWNLOADS}/$BASE_DIRECTORY/$folderName"
            folderPath = relativeDir
            fileName = "metrics.csv"

            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                put(MediaStore.Downloads.RELATIVE_PATH, relativeDir)
            }

            val uri = context.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: throw IllegalStateException("Failed to create file in Downloads")

            outputStream = context.contentResolver.openOutputStream(uri, "wa")
                ?: throw IllegalStateException("Failed to open output stream")

            // Write header
            outputStream!!.write(CSV_HEADER.toByteArray())
            outputStream!!.flush()

            return relativeDir
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
                append(String.format(Locale.US, "%.1f", snapshot.cpuUsagePercent)).append(',')
                append(String.format(Locale.US, "%.1f", snapshot.appMemoryMb)).append(',')
                append(String.format(Locale.US, "%.1f", snapshot.availableRamMb))
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
            val relativeDir = folderPath ?: return
            
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, "summary.txt")
                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                put(MediaStore.Downloads.RELATIVE_PATH, relativeDir)
            }
            
            try {
                val uri = context.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    contentValues
                )
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri, "w")?.use {
                        it.write(summaryText.toByteArray())
                    }
                }
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
