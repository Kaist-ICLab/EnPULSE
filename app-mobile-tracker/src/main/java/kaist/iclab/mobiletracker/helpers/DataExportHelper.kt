package kaist.iclab.mobiletracker.helpers

import android.content.Context
import android.util.Log
import kaist.iclab.mobiletracker.services.upload.handlers.SensorUploadHandler
import kaist.iclab.mobiletracker.services.upload.handlers.SensorUploadHandlerRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Helper class to export all sensor data from the Room database to a ZIP file containing CSVs.
 * This provides a manual backup flow for researchers.
 */
class DataExportHelper(
    private val handlerRegistry: SensorUploadHandlerRegistry
) {
    companion object {
        private const val TAG = "DataExportHelper"
        private const val BATCH_SIZE = 1000
    }

    /**
     * Export all sensor data to a ZIP file.
     * @param context Android context
     * @return The resulting ZIP File, or null if export failed
     */
    suspend fun exportAllData(context: Context): File? = withContext(Dispatchers.IO) {
        val tempDir = File(context.cacheDir, "data_export_${System.currentTimeMillis()}")
        if (!tempDir.exists() && !tempDir.mkdirs()) {
            Log.e(TAG, "Failed to create temp directory for export")
            return@withContext null
        }

        try {
            val csvFiles = mutableListOf<File>()
            val handlers = handlerRegistry.getAllHandlers()

            for (handler in handlers) {
                val sensorId = handler.sensorId
                val csvFile = File(tempDir, "${sensorId}.csv")

                if (exportSensorToCsv(handler, csvFile)) {
                    csvFiles.add(csvFile)
                }
            }

            if (csvFiles.isEmpty()) {
                Log.w(TAG, "No data found to export")
                return@withContext null
            }

            // Create ZIP file in the external files directory
            val exportDir = context.getExternalFilesDir("exports") ?: context.filesDir
            if (!exportDir.exists()) exportDir.mkdirs()

            // Clean up previous export files
            exportDir.listFiles()?.forEach { it.delete() }

            val timestamp =
                SimpleDateFormat("yyyyMMdd_HH:mm:ss", Locale.getDefault()).format(Date())
            val zipFile = File(exportDir, "Data_${timestamp}.zip")
            createZip(csvFiles, zipFile)

            Log.i(TAG, "Export completed successfully: ${zipFile.absolutePath}")
            zipFile
        } catch (e: Exception) {
            Log.e(TAG, "Error during data export", e)
            null
        } finally {
            // Cleanup temp files
            tempDir.deleteRecursively()
        }
    }

    private suspend fun exportSensorToCsv(
        handler: SensorUploadHandler,
        file: File
    ): Boolean {
        return try {
            val totalCount = handler.getRecordCount()
            if (totalCount == 0) return false

            FileWriter(file).use { writer ->
                // Write header
                writer.write(handler.getCsvHeader())
                writer.write("\n")

                var offset = 0
                while (offset < totalCount) {
                    val records = handler.getRecordsPaginated(BATCH_SIZE, offset)
                    if (records.isEmpty()) break

                    for (record in records) {
                        writer.write(handler.recordToCsvRow(record))
                        writer.write("\n")
                    }
                    offset += BATCH_SIZE
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export sensor ${handler.sensorId}", e)
            false
        }
    }

    private fun createZip(files: List<File>, zipFile: File) {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
            files.forEach { file ->
                zos.putNextEntry(ZipEntry(file.name))
                file.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
    }
}
