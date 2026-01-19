package kaist.iclab.mobiletracker.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import kaist.iclab.mobiletracker.repository.SensorRecord
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Helper class for exporting sensor data to CSV format.
 */
object CsvExportHelper {
    
    private const val TAG = "CsvExportHelper"
    
    /**
     * Export sensor records to a CSV file.
     * 
     * @param context Android context
     * @param sensorName Name of the sensor (used in filename)
     * @param records List of sensor records to export
     * @return Uri of the created file, or null if export failed
     */
    fun exportToCsv(
        context: Context,
        sensorName: String,
        records: List<SensorRecord>
    ): Uri? {
        if (records.isEmpty()) {
            Log.w(TAG, "No records to export")
            return null
        }
        
        try {
            // Create export directory
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }
            
            // Generate filename with timestamp
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val sanitizedName = sensorName.replace(" ", "_").replace("/", "_")
            val fileName = "${sanitizedName}_$timestamp.csv"
            val file = File(exportDir, fileName)
            
            // Get all unique field names from records
            val allFieldNames = records
                .flatMap { it.fields.keys }
                .distinct()
                .sorted()
            
            // Write CSV
            FileWriter(file).use { writer ->
                // Write header
                val header = listOf("id", "timestamp") + allFieldNames
                writer.write(header.joinToString(","))
                writer.write("\n")
                
                // Write data rows
                records.forEach { record ->
                    val timestampStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
                        .format(Date(record.timestamp))
                    
                    val row = listOf(
                        record.id.toString(),
                        timestampStr
                    ) + allFieldNames.map { fieldName ->
                        escapeCsvField(record.fields[fieldName] ?: "")
                    }
                    
                    writer.write(row.joinToString(","))
                    writer.write("\n")
                }
            }
            
            Log.d(TAG, "Exported ${records.size} records to ${file.absolutePath}")
            
            // Return file URI using FileProvider
            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export CSV: ${e.message}", e)
            return null
        }
    }
    
    /**
     * Export multiple sensors to separate CSV files and return as a list of URIs.
     */
    fun exportMultipleSensorsToCsv(
        context: Context,
        sensorData: Map<String, List<SensorRecord>>
    ): List<Uri> {
        return sensorData.mapNotNull { (sensorName, records) ->
            exportToCsv(context, sensorName, records)
        }
    }
    
    /**
     * Share a CSV file using Android's share intent.
     */
    fun shareCsv(context: Context, uri: Uri, sensorName: String) {
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
    fun shareMultipleCsv(context: Context, uris: List<Uri>, title: String = "Sensor Data Export") {
        if (uris.isEmpty()) return
        
        if (uris.size == 1) {
            shareCsv(context, uris.first(), title)
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
}
