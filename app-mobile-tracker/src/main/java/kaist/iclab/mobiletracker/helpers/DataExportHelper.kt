package kaist.iclab.mobiletracker.helpers

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
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
 * Helper class to export all sensor data from the ObjectBox database to a ZIP file containing CSVs.
 * This provides a manual backup flow for researchers.
 *
 * Holds the application [Context] (injected via DI) so callers such as ViewModels don't need to
 * keep a Context reference of their own.
 */
class DataExportHelper(
    private val context: Context,
    private val handlerRegistry: SensorUploadHandlerRegistry
) {
    companion object {
        private const val TAG = "DataExportHelper"
        private const val BATCH_SIZE = 1000
    }

    /**
     * Export all sensor data to a ZIP file.
     * @return The resulting ZIP File, or null if export failed
     */
    suspend fun exportAllData(): File? = withContext(Dispatchers.IO) {
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

            withContext(Dispatchers.IO) {
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
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export sensor ${handler.sensorId}", e)
            false
        }
    }

    /**
     * Share an exported ZIP file via Android's share sheet.
     *
     * Uses [FileProvider] to grant read access and launches a chooser. Runs from the application
     * context (with [Intent.FLAG_ACTIVITY_NEW_TASK]) so it can be invoked outside an Activity.
     *
     * @throws Exception if the file cannot be shared; callers are expected to handle failures.
     */
    fun shareZip(file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Mobile Tracker Data Export")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "Share Data Export")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
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
