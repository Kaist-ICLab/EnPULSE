package kaist.iclab.mobiletracker.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import kaist.iclab.mobiletracker.repository.SensorRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Helper class for exporting sensor data to CSV format.
 *
 * Holds the application [Context] (injected via DI) so callers such as ViewModels
 * don't need to keep a Context reference of their own.
 */
class CsvExportHelper(
    private val context: Context
) {

    /**
     * Export sensor records to a CSV file, streaming in batches so memory use stays bounded by
     * [batchSize] rather than the full record count — a "big" sensor table (e.g. watch
     * accelerometer) can be millions of rows, and materializing them all as [SensorRecord] before
     * writing a single byte is what was causing OOMs here.
     *
     * The header is derived from the first non-empty batch: every entity's `toRecord()` builds its
     * `fields` map from a fixed set of literal keys, so the key set is the same for every record of
     * a given sensor and a single batch is enough to determine it.
     *
     * @param sensorName Name of the sensor (used in filename)
     * @param totalCount Total number of records to export (drives when to stop paging)
     * @param batchSize Number of records to fetch and write per batch
     * @param fetchBatch Suspending page fetcher: (offset, limit) -> records
     * @return Uri of the created file, or null if export failed
     */
    suspend fun exportToCsv(
        sensorName: String,
        totalCount: Int,
        batchSize: Int = 5000,
        fetchBatch: suspend (offset: Int, limit: Int) -> List<SensorRecord>
    ): Uri? = withContext(Dispatchers.IO) {
        if (totalCount <= 0) {
            Log.w(TAG, "No records to export")
            return@withContext null
        }

        try {
            // Create export directory
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            // Clean up previous export files
            exportDir.listFiles()?.forEach { it.delete() }

            // Generate filename with timestamp
            val timestamp =
                SimpleDateFormat("yyyyMMdd_HH:mm:ss", Locale.getDefault()).format(Date())
            val sanitizedName = sensorName.replace(" ", "").replace("/", "")
            val fileName = "${sanitizedName}_$timestamp.csv"
            val file = File(exportDir, fileName)

            var fieldNames: List<String>? = null
            var written = 0

            FileWriter(file).use { writer ->
                var offset = 0
                while (offset < totalCount) {
                    val batch = fetchBatch(offset, batchSize)
                    if (batch.isEmpty()) break

                    if (fieldNames == null) {
                        fieldNames = batch.first().fields.keys.sorted()
                        val header = listOf("id", "timestamp") + fieldNames!!
                        writer.write(header.joinToString(","))
                        writer.write("\n")
                    }

                    batch.forEach { record ->
                        val timestampStr =
                            SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
                                .format(Date(record.timestamp))

                        val row = listOf(
                            record.id.toString(),
                            timestampStr
                        ) + fieldNames!!.map { fieldName ->
                            escapeCsvField(record.fields[fieldName] ?: "")
                        }

                        writer.write(row.joinToString(","))
                        writer.write("\n")
                    }

                    written += batch.size
                    offset += batchSize
                }
            }

            if (written == 0) {
                Log.w(TAG, "No records to export")
                file.delete()
                return@withContext null
            }

            Log.d(TAG, "Exported $written records to ${file.absolutePath}")

            // Return file URI using FileProvider
            return@withContext FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export CSV: ${e.message}", e)
            return@withContext null
        }
    }

    /**
     * Export multiple sensors (already loaded in memory) to separate CSV files and return as a
     * list of URIs. Intended for small, already-fetched record sets — for large per-sensor tables,
     * page through [exportToCsv]'s `fetchBatch` instead of building the full list first.
     */
    suspend fun exportMultipleSensorsToCsv(
        sensorData: Map<String, List<SensorRecord>>
    ): List<Uri> {
        return sensorData.mapNotNull { (sensorName, records) ->
            exportToCsv(sensorName, records.size) { offset, _ ->
                if (offset == 0) records else emptyList()
            }
        }
    }

    /**
     * Share a CSV file using Android's share intent.
     */
    fun shareCsv(uri: Uri, sensorName: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "$sensorName Data Export")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "Share CSV")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    /**
     * Share multiple CSV files.
     */
    fun shareMultipleCsv(uris: List<Uri>, title: String = "Sensor Data Export") {
        if (uris.isEmpty()) return

        if (uris.size == 1) {
            shareCsv(uris.first(), title)
            return
        }

        val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "text/csv"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "Share CSV Files")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    /**
     * Escape a field value for CSV format.
     * Wraps in quotes if contains comma, newline, or quote.
     */
    private fun escapeCsvField(value: String): String {
        return if (value.contains(",") || value.contains("\n") || value.contains("\"")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    companion object {
        private const val TAG = "CsvExportHelper"
    }
}
